/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.clan;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.clan.OnClanWarStart;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SurrenderPledgeWar;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Sdw
 */
public class ClanWar
{
	public static final long TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR = TimeUnit.DAYS.toMillis(7);
	public static final long TIME_TO_DELETION_AFTER_CANCELLATION = TimeUnit.DAYS.toMillis(5);
	public static final long TIME_TO_DELETION_AFTER_DEFEAT = TimeUnit.DAYS.toMillis(21);
	private final int _attackerClanId;
	private final int _attackedClanId;
	private int _winnerClanId = 0;
	private ClanWarState _state;
	private Future<?> _cancelTask;
	private final long _startTime;
	private long _endTime = 0;
	
	public static enum WarProgress
	{
		VERY_LOW,
		LOW,
		NORMAL,
		HIGH,
		VERY_HIGH;
	}
	
	private final AtomicInteger _attackerKillCount = new AtomicInteger();
	private final AtomicInteger _attackedKillCount = new AtomicInteger();
	
	private boolean _attackerDeclared;
	private boolean _attackedDeclared;
	
	public ClanWar(Clan attacker, Clan attacked)
	{
		_attackerClanId = attacker.getId();
		_attackedClanId = attacked.getId();
		_startTime = System.currentTimeMillis();
		_state = ClanWarState.BLOOD_DECLARATION;
		_cancelTask = ThreadPool.schedule(this::clanWarTimeout, (_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis());
		attacker.addWar(attacked.getId(), this);
		attacked.addWar(attacker.getId(), this);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CLAN_WAR_START))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnClanWarStart(attacker, attacked));
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DECLARED_A_CLAN_WAR_WITH_S1);
		sm.addString(attacked.getName());
		attacker.broadcastToOnlineMembers(sm);
		
		sm = new SystemMessage(SystemMessageId.S1_HAS_DECLARED_A_CLAN_WAR_THE_WAR_WILL_AUTOMATICALLY_START_IF_YOU_KILL_MORE_THAN_5_S1_CLAN_MEMBERS_IN_A_WEEK);
		sm.addString(attacker.getName());
		attacked.broadcastToOnlineMembers(sm);
	}
	
	public ClanWar(Clan attacker, Clan attacked, int attackerKillCount, int attackedKillCount, int winnerClan, long startTime, long endTime, ClanWarState state)
	{
		_attackerClanId = attacker.getId();
		_attackedClanId = attacked.getId();
		_startTime = startTime;
		_endTime = endTime;
		_state = state;
		_attackerKillCount.set(attackerKillCount);
		_attackedKillCount.set(attackedKillCount);
		_winnerClanId = winnerClan;
		if ((_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) > System.currentTimeMillis())
		{
			_cancelTask = ThreadPool.schedule(this::clanWarTimeout, (_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis());
		}
		
		if (_endTime > 0)
		{
			final long endTimePeriod = _endTime + (_state == ClanWarState.TIE ? TIME_TO_DELETION_AFTER_CANCELLATION : TIME_TO_DELETION_AFTER_DEFEAT);
			if (endTimePeriod > System.currentTimeMillis())
			{
				ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(_attackerClanId, _attackedClanId), 10000);
			}
			else
			{
				ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(_attackerClanId, _attackedClanId), endTimePeriod);
			}
		}
	}
	
	public void onKill(Player killer, Player victim)
	{
		final Clan victimClan = victim.getClan();
		final Clan killerClan = killer.getClan();
		
		// Reputation increase by killing an enemy (over level 4) in a clan war under the condition of mutual war declaration
		if ((victim.getLevel() > 4) && (_state == ClanWarState.MUTUAL))
		{
			// however, when the other side reputation score is 0 or below, your clan cannot acquire any reputation points from them.
			if (victimClan.getReputationScore() > 0)
			{
				victimClan.takeReputationScore(Config.REPUTATION_SCORE_PER_KILL);
				killerClan.addReputationScore(Config.REPUTATION_SCORE_PER_KILL);
			}
			
			// System Message notification to clan members
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_KILLED_BY_A_MEMBER_OF_THE_S2_CLAN_CLAN_REPUTATION_POINTS_1);
			sm.addPcName(victim);
			sm.addString(killerClan.getName());
			victimClan.broadcastToOnlineMembers(sm);
			sm = new SystemMessage(SystemMessageId.A_MEMBER_OF_THE_S1_CLAN_IS_KILLED_BY_C2_CLAN_REPUTATION_POINTS_1);
			sm.addString(victimClan.getName());
			sm.addPcName(killer);
			killerClan.broadcastToOnlineMembers(sm);
			if (killerClan.getId() == _attackerClanId)
			{
				_attackerKillCount.incrementAndGet();
			}
			else
			{
				_attackedKillCount.incrementAndGet();
			}
		}
		else if ((_state == ClanWarState.BLOOD_DECLARATION) && (victimClan.getId() == _attackerClanId) && (victim.getReputation() >= 0))
		{
			final int killCount = _attackedKillCount.incrementAndGet();
			if (killCount >= 5)
			{
				_state = ClanWarState.MUTUAL;
				SystemMessage sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_500_CLAN_REPUTATION_POINTS_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
				sm.addString(victimClan.getName());
				killerClan.broadcastToOnlineMembers(sm);
				
				sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_500_CLAN_REPUTATION_POINTS_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
				sm.addString(killerClan.getName());
				victimClan.broadcastToOnlineMembers(sm);
				
				if (_cancelTask != null)
				{
					_cancelTask.cancel(true);
					_cancelTask = null;
				}
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.A_CLAN_MEMBER_OF_S1_WAS_KILLED_BY_YOUR_CLAN_MEMBER_IF_YOUR_CLAN_KILLS_S2_MEMBERS_OF_CLAN_S1_A_CLAN_WAR_WITH_CLAN_S1_WILL_START);
				sm.addString(victimClan.getName());
				sm.addInt(5 - killCount);
				killerClan.broadcastToOnlineMembers(sm);
			}
		}
	}
	
	public void cancel(Player player, Clan cancelor)
	{
		final Clan winnerClan = cancelor.getId() == _attackerClanId ? ClanTable.getInstance().getClan(_attackedClanId) : ClanTable.getInstance().getClan(_attackerClanId);
		
		// Reduce reputation.
		cancelor.takeReputationScore(500);
		player.sendPacket(new SurrenderPledgeWar(cancelor.getName(), player.getName()));
		SystemMessage sm = new SystemMessage(SystemMessageId.THE_WAR_IS_OVER_AS_YOU_VE_ADMITTED_DEFEAT_FROM_THE_CLAN_S1_YOU_VE_LOST);
		sm.addString(winnerClan.getName());
		cancelor.broadcastToOnlineMembers(sm);
		
		sm = new SystemMessage(SystemMessageId.THE_WAR_ENDED_BY_THE_S1_CLAN_S_DEFEAT_DECLARATION_YOU_HAVE_WON_THE_CLAN_WAR_OVER_THE_S1_CLAN);
		sm.addString(cancelor.getName());
		winnerClan.broadcastToOnlineMembers(sm);
		
		_winnerClanId = winnerClan.getId();
		_endTime = System.currentTimeMillis();
		ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(cancelor.getId(), winnerClan.getId()), 5000 /* (_endTime + TIME_TO_DELETION_AFTER_DEFEAT) - System.currentTimeMillis() */);
	}
	
	public void clanWarTimeout()
	{
		final Clan attackerClan = ClanTable.getInstance().getClan(_attackerClanId);
		final Clan attackedClan = ClanTable.getInstance().getClan(_attackedClanId);
		if ((attackerClan != null) && (attackedClan != null))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THE_WAR_DECLARED_BY_THE_S1_CLAN_HAS_ENDED);
			sm.addString(attackerClan.getName());
			attackedClan.broadcastToOnlineMembers(sm);
			
			sm = new SystemMessage(SystemMessageId.BECAUSE_CLAN_S1_DID_NOT_FIGHT_BACK_FOR_1_WEEK_THE_CLAN_WAR_WAS_CANCELLED);
			sm.addString(attackedClan.getName());
			attackerClan.broadcastToOnlineMembers(sm);
			
			_state = ClanWarState.TIE;
			_endTime = System.currentTimeMillis();
			ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(attackerClan.getId(), attackedClan.getId()), 5000 /* (_endTime + TIME_TO_DELETION_AFTER_CANCELLATION) - System.currentTimeMillis() */);
		}
	}
	
	public void mutualClanWarAccepted(Clan attacker, Clan attacked)
	{
		if (attacker.getId() == _attackerClanId)
		{
			_attackerDeclared = true;
		}
		else if (attacker.getId() == _attackedClanId)
		{
			_attackedDeclared = true;
		}
		
		if (_attackerDeclared && _attackedDeclared)
		{
			_state = ClanWarState.MUTUAL;
			SystemMessage sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_500_CLAN_REPUTATION_POINTS_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
			sm.addString(attacker.getName());
			attacked.broadcastToOnlineMembers(sm);
			
			sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_500_CLAN_REPUTATION_POINTS_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
			sm.addString(attacked.getName());
			attacker.broadcastToOnlineMembers(sm);
			
			if (_cancelTask != null)
			{
				_cancelTask.cancel(true);
				_cancelTask = null;
			}
		}
	}
	
	public int getKillDifference(Clan clan)
	{
		return _attackerClanId == clan.getId() ? _attackerKillCount.get() - _attackedKillCount.get() : _attackedKillCount.get() - _attackerKillCount.get();
	}
	
	public ClanWarState getClanWarState(Clan clan)
	{
		if (_winnerClanId > 0)
		{
			return _winnerClanId == clan.getId() ? ClanWarState.WIN : ClanWarState.LOSS;
		}
		return _state;
	}
	
	public WarProgress calculateWarProgress(Clan clan)
	{
		int pointDiff = getKillDifference(clan);
		if (pointDiff <= -50)
		{
			return WarProgress.VERY_LOW;
		}
		
		if ((pointDiff > -50) && (pointDiff <= -20))
		{
			return WarProgress.LOW;
		}
		
		if ((pointDiff > -20) && (pointDiff <= 19))
		{
			return WarProgress.NORMAL;
		}
		
		if ((pointDiff > 19) && (pointDiff <= 49))
		{
			return WarProgress.HIGH;
		}
		
		return WarProgress.VERY_HIGH;
	}
	
	public int getAttackerClanId()
	{
		return _attackerClanId;
	}
	
	public int getAttackedClanId()
	{
		return _attackedClanId;
	}
	
	public int getAttackerKillCount()
	{
		return _attackerKillCount.get();
	}
	
	public int getAttackedKillCount()
	{
		return _attackedKillCount.get();
	}
	
	public int getWinnerClanId()
	{
		return _winnerClanId;
	}
	
	public long getStartTime()
	{
		return _startTime;
	}
	
	public long getEndTime()
	{
		return _endTime;
	}
	
	public ClanWarState getState()
	{
		return _state;
	}
	
	public int getKillToStart()
	{
		return _state == ClanWarState.BLOOD_DECLARATION ? 5 - _attackedKillCount.get() : 0;
	}
	
	public int getRemainingTime()
	{
		return (int) (((_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis()) / 1000L);
	}
	
	public Clan getOpposingClan(Clan clan)
	{
		return _attackerClanId == clan.getId() ? ClanTable.getInstance().getClan(_attackedClanId) : ClanTable.getInstance().getClan(_attackerClanId);
	}
}
