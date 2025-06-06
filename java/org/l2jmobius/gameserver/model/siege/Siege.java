/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.model.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SiegeScheduleData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.SiegeGuardManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.SiegeClan;
import org.l2jmobius.gameserver.model.SiegeScheduleDate;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.TowerSpawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.instance.ControlTower;
import org.l2jmobius.gameserver.model.actor.instance.FlameTower;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnCastleSiegeStart;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

public class Siege implements Siegable
{
	protected static final Logger LOGGER = Logger.getLogger(Siege.class.getName());
	
	// Type ids.
	public static final byte OWNER = -1;
	public static final byte DEFENDER = 0;
	public static final byte ATTACKER = 1;
	public static final byte DEFENDER_NOT_APPROVED = 2;
	
	private int _controlTowerCount;
	
	// Must support concurrent modifications.
	private final Collection<SiegeClan> _attackerClans = ConcurrentHashMap.newKeySet();
	private final Collection<SiegeClan> _defenderClans = ConcurrentHashMap.newKeySet();
	private final Collection<SiegeClan> _defenderWaitingClans = ConcurrentHashMap.newKeySet();
	
	// Castle setting.
	private final List<ControlTower> _controlTowers = new ArrayList<>();
	private final List<Npc> _relic = new ArrayList<>();
	private final List<FlameTower> _flameTowers = new ArrayList<>();
	final Castle _castle;
	boolean _isInProgress = false;
	private boolean _isNormalSide = true; // true = Atk is Atk, false = Atk is Def
	protected boolean _isRegistrationOver = false;
	protected Calendar _siegeEndDate;
	protected ScheduledFuture<?> _scheduledStartSiegeTask = null;
	protected ScheduledFuture<?> _scheduledSiegeInfoTask = null;
	protected int _firstOwnerClanId = -1;
	
	public Siege(Castle castle)
	{
		_castle = castle;
		
		final SiegeScheduleDate schedule = SiegeScheduleData.getInstance().getScheduleDateForCastleId(_castle.getResidenceId());
		if ((schedule != null) && schedule.siegeEnabled())
		{
			startAutoTask();
		}
	}
	
	public class ScheduleEndSiegeTask implements Runnable
	{
		private final Castle _castleInst;
		
		public ScheduleEndSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}
		
		@Override
		public void run()
		{
			if (!_isInProgress)
			{
				return;
			}
			
			try
			{
				final long timeRemaining = _siegeEndDate.getTimeInMillis() - System.currentTimeMillis();
				if (timeRemaining > 3600000)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_H);
					sm.addInt(2);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 3600000); // Prepare task for 1 hr left.
				}
				else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_MIN);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_MIN);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_MIN);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CASTLE_SIEGE_ENDS_IN_S1_SEC);
					sm.addInt((int) timeRemaining / 1000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down
				}
				else
				{
					_castleInst.getSiege().endSiege();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": ", e);
			}
		}
	}
	
	public class ScheduleStartSiegeTask implements Runnable
	{
		private final Castle _castleInst;
		
		public ScheduleStartSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}
		
		@Override
		public void run()
		{
			_scheduledStartSiegeTask.cancel(false);
			if (_isInProgress)
			{
				return;
			}
			
			try
			{
				final long currentTime = System.currentTimeMillis();
				if (!_castle.isTimeRegistrationOver())
				{
					final long regTimeRemaining = getTimeRegistrationOverDate().getTimeInMillis() - currentTime;
					if (regTimeRemaining > 0)
					{
						_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), regTimeRemaining);
						return;
					}
					endTimeRegistration(true);
				}
				
				final long timeRemaining = getSiegeDate().getTimeInMillis() - currentTime;
				if (timeRemaining > 86400000)
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 86400000); // Prepare task for 24 before siege start to end registration
				}
				else if ((timeRemaining <= 86400000) && (timeRemaining > 13600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
					sm.addCastleId(_castle.getResidenceId());
					Broadcast.toAllOnlinePlayers(sm);
					_isRegistrationOver = true;
					clearSiegeWaitingClan();
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 13600000); // Prepare task for 1 hr left before siege start.
				}
				else if ((timeRemaining <= 13600000) && (timeRemaining > 600000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down
				}
				else
				{
					_castleInst.getSiege().startSiege();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": ", e);
			}
		}
	}
	
	@Override
	public void endSiege()
	{
		if (_isInProgress)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THE_S1_SIEGE_HAS_FINISHED);
			sm.addCastleId(_castle.getResidenceId());
			Broadcast.toAllOnlinePlayers(sm);
			Broadcast.toAllOnlinePlayers(new PlaySound("systemmsg_eu.18"));
			if (_castle.getOwnerId() > 0)
			{
				final Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
				sm = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
				sm.addString(clan.getName());
				sm.addCastleId(_castle.getResidenceId());
				Broadcast.toAllOnlinePlayers(sm);
				
				if (clan.getId() == _firstOwnerClanId)
				{
					// Owner is unchanged
					clan.increaseBloodAllianceCount();
				}
				else
				{
					_castle.setTicketBuyCount(0);
					for (ClanMember member : clan.getMembers())
					{
						if (member != null)
						{
							final Player player = member.getPlayer();
							if ((player != null) && player.isNoble())
							{
								Hero.getInstance().setCastleTaken(player.getObjectId(), getCastle().getResidenceId());
							}
						}
					}
				}
				
				long reward = (getCastle().getTempTreasury() * clan.getRewardMercenary()) / 100;
				getCastle().updateTempTreasure(getCastle().getTempTreasury() - reward);
				final int winnersCount = clan.getMapMercenary().size();
				if (winnersCount != 0)
				{
					reward = reward / winnersCount;
					for (Integer elem : clan.getMapMercenary().keySet())
					{
						final Message msg = new Message(elem, "Reward from Siege!", "Your reward mercenary.", MailType.REGULAR);
						final Mail attachments = msg.createAttachments();
						attachments.addItem(ItemProcessType.REWARD, 57, reward, null, null);
						MailManager.getInstance().sendMessage(msg);
					}
				}
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
				sm.addCastleId(_castle.getResidenceId());
				Broadcast.toAllOnlinePlayers(sm);
			}
			
			for (SiegeClan attackerClan : getAttackerClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(attackerClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				for (Player member : clan.getOnlineMembers(0))
				{
					member.checkItemRestriction();
				}
				
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
				clan.setRecruitMercenary(false);
				clan.removeMercenaryByClanId(attackerClan.getClanId());
			}
			
			for (SiegeClan defenderClan : getDefenderClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(defenderClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				for (Player member : clan.getOnlineMembers(0))
				{
					member.checkItemRestriction();
				}
				
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
				clan.setRecruitMercenary(false);
				clan.removeMercenaryByClanId(defenderClan.getClanId());
			}
			
			_castle.updateClansReputation();
			removeFlags(); // Removes all flags. Note: Remove flag before teleporting players
			teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN); // Teleport to the second closest town
			_isInProgress = false; // Flag so that siege instance can be started
			updatePlayerSiegeStateFlags(true);
			saveCastleSiege(); // Save castle specific data
			clearSiegeClan(); // Clear siege clan from db
			removeTowers(); // Remove all towers from this castle
			SiegeGuardManager.getInstance().unspawnSiegeGuard(getCastle()); // Remove all spawned siege guard from this castle
			if (_castle.getOwnerId() > 0)
			{
				SiegeGuardManager.getInstance().removeSiegeGuards(getCastle());
			}
			_castle.spawnDoor(); // Respawn door to castle
			_castle.setFirstMidVictory(false);
			_castle.getZone().setActive(false);
			_castle.getZone().updateZoneStatusForCharactersInside();
			_castle.getZone().setSiegeInstance(null);
			
			// Notify to scripts.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_FINISH, getCastle()))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeFinish(this), getCastle());
			}
		}
	}
	
	private void removeDefender(SiegeClan sc)
	{
		if (sc != null)
		{
			getDefenderClans().remove(sc);
		}
	}
	
	private void removeAttacker(SiegeClan sc)
	{
		if (sc != null)
		{
			getAttackerClans().remove(sc);
		}
	}
	
	private void addDefender(SiegeClan sc, SiegeClanType type)
	{
		if (sc == null)
		{
			return;
		}
		sc.setType(type);
		getDefenderClans().add(sc);
	}
	
	private void addAttacker(SiegeClan sc)
	{
		if (sc == null)
		{
			return;
		}
		sc.setType(SiegeClanType.ATTACKER);
		getAttackerClans().add(sc);
	}
	
	/**
	 * When control of castle changed during siege.
	 */
	public void midVictory()
	{
		if (_isInProgress) // Siege still in progress
		{
			if (_castle.getOwnerId() > 0)
			{
				SiegeGuardManager.getInstance().removeSiegeGuards(getCastle()); // Remove all merc entry from db
			}
			
			if (getDefenderClans().isEmpty() && // If defender doesn't exist (Pc vs Npc)
				(getAttackerClans().size() == 1 // Only 1 attacker
				))
			{
				final SiegeClan scNewOwner = getAttackerClan(_castle.getOwnerId());
				removeAttacker(scNewOwner);
				addDefender(scNewOwner, SiegeClanType.OWNER);
				endSiege();
				return;
			}
			if (_castle.getOwnerId() > 0)
			{
				// If defender doesn't exist (Pc vs Npc) and only an alliance attacks
				final int allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
				if (getDefenderClans().isEmpty() && (allyId != 0))
				{
					boolean allinsamealliance = true;
					for (SiegeClan sc : getAttackerClans())
					{
						if ((sc != null) && (ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId))
						{
							allinsamealliance = false;
						}
					}
					if (allinsamealliance)
					{
						final SiegeClan scNewOwner = getAttackerClan(_castle.getOwnerId());
						removeAttacker(scNewOwner);
						addDefender(scNewOwner, SiegeClanType.OWNER);
						endSiege();
						return;
					}
				}
				
				for (SiegeClan sc : getDefenderClans())
				{
					if (sc != null)
					{
						removeDefender(sc);
						addAttacker(sc);
					}
				}
				
				final SiegeClan scNewOwner = getAttackerClan(_castle.getOwnerId());
				removeAttacker(scNewOwner);
				addDefender(scNewOwner, SiegeClanType.OWNER);
				
				// The player's clan is in an alliance
				for (Clan clan : ClanTable.getInstance().getClanAllies(allyId))
				{
					final SiegeClan sc = getAttackerClan(clan.getId());
					if (sc != null)
					{
						removeAttacker(sc);
						addDefender(sc, SiegeClanType.DEFENDER);
					}
				}
				_castle.setFirstMidVictory(true);
				teleportPlayer(SiegeTeleportWhoType.Attacker, TeleportWhereType.SIEGEFLAG); // Teleport to the second closest town
				teleportPlayer(SiegeTeleportWhoType.Spectator, TeleportWhereType.TOWN); // Teleport to the second closest town
				removeDefenderFlags(); // Removes defenders' flags
				_castle.removeUpgrade(); // Remove all castle upgrade
				_castle.spawnDoor(true); // Respawn door to castle but make them weaker (50% hp)
				removeTowers(); // Remove all towers from this castle
				SiegeGuardManager.getInstance().unspawnSiegeGuard(getCastle()); // Remove all spawned siege guard from this castle.
				if (_castle.getOwnerId() > 0)
				{
					SiegeGuardManager.getInstance().removeSiegeGuards(getCastle());
				}
				_controlTowerCount = 0; // Each new siege midvictory CT are completely respawned.
				spawnControlTower();
				spawnFlameTower();
				updatePlayerSiegeStateFlags(false);
				
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, getCastle()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeOwnerChange(this), getCastle());
				}
			}
		}
	}
	
	/**
	 * When siege starts.
	 */
	@Override
	public void startSiege()
	{
		if (!_isInProgress)
		{
			_firstOwnerClanId = _castle.getOwnerId();
			if (getAttackerClans().isEmpty())
			{
				SystemMessage sm;
				if (_firstOwnerClanId <= 0)
				{
					sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S_SIEGE_WAS_CANCELED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED);
					final Clan ownerClan = ClanTable.getInstance().getClan(_firstOwnerClanId);
					ownerClan.increaseBloodAllianceCount();
				}
				sm.addCastleId(_castle.getResidenceId());
				Broadcast.toAllOnlinePlayers(sm);
				saveCastleSiege();
				return;
			}
			
			_isNormalSide = true; // Atk is now atk
			_isInProgress = true; // Flag so that same siege instance cannot be started again
			loadSiegeClan(); // Load siege clan from db
			updatePlayerSiegeStateFlags(false);
			updatePlayerSiegeStateFlags(false); // This fixes icons between allies because it first shows as an enemy for unknown reasons
			teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN); // Teleport to the closest town
			_controlTowerCount = 0;
			spawnRelic();
			spawnControlTower(); // Spawn control tower
			spawnFlameTower(); // Spawn control tower
			_castle.spawnDoor(); // Spawn door
			spawnSiegeGuard(); // Spawn siege guard
			SiegeGuardManager.getInstance().deleteTickets(getCastle().getResidenceId()); // remove the tickets from the ground
			_castle.getZone().setSiegeInstance(this);
			_castle.getZone().setActive(true);
			_castle.getZone().updateZoneStatusForCharactersInside();
			
			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, SiegeManager.getInstance().getSiegeLength());
			ThreadPool.schedule(new ScheduleEndSiegeTask(_castle), 1000); // Prepare auto end task
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_THE_SIEGE_HAS_BEGUN);
			sm.addCastleId(_castle.getResidenceId());
			Broadcast.toAllOnlinePlayers(sm);
			Broadcast.toAllOnlinePlayers(new PlaySound("systemmsg_eu.17"));
			for (Player player : World.getInstance().getPlayers())
			{
				SiegeManager.getInstance().sendSiegeInfo(player);
			}
			
			// Notify to scripts.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_START, getCastle()))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeStart(this), getCastle());
			}
		}
	}
	
	/**
	 * Announce to player.
	 * @param message The SystemMessage to send to player
	 * @param bothSides True - broadcast to both attackers and defenders. False - only to defenders.
	 */
	public void announceToPlayer(SystemMessage message, boolean bothSides)
	{
		for (SiegeClan siegeClans : getDefenderClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClans.getClanId());
			if (clan != null)
			{
				for (Player member : clan.getOnlineMembers(0))
				{
					member.sendPacket(message);
				}
			}
		}
		
		if (bothSides)
		{
			for (SiegeClan siegeClans : getAttackerClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(siegeClans.getClanId());
				if (clan != null)
				{
					for (Player member : clan.getOnlineMembers(0))
					{
						member.sendPacket(message);
					}
				}
			}
		}
	}
	
	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		Clan clan;
		for (SiegeClan siegeclan : getAttackerClans())
		{
			if (siegeclan == null)
			{
				continue;
			}
			
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 1);
					member.setSiegeSide(_castle.getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(Config.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, Config.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.updateUserInfo();
				World.getInstance().forEachVisibleObject(member, Player.class, player ->
				{
					if (!member.isVisibleFor(player))
					{
						return;
					}
					
					final long relation = member.getRelation(player);
					final boolean isAutoAttackable = member.isAutoAttackable(player);
					final RelationCache oldrelation = member.getKnownRelations().get(player.getObjectId());
					if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
					{
						final RelationChanged rc = new RelationChanged();
						rc.addRelation(member, relation, isAutoAttackable);
						if (member.hasSummon())
						{
							final Summon pet = member.getPet();
							if (pet != null)
							{
								rc.addRelation(pet, relation, isAutoAttackable);
							}
							if (member.hasServitors())
							{
								member.getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
							}
						}
						player.sendPacket(rc);
						member.getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
					}
				});
			}
		}
		for (SiegeClan siegeclan : getDefenderClans())
		{
			if (siegeclan == null)
			{
				continue;
			}
			
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 2);
					member.setSiegeSide(_castle.getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(Config.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, Config.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.updateUserInfo();
				World.getInstance().forEachVisibleObject(member, Player.class, player ->
				{
					if (!member.isVisibleFor(player))
					{
						return;
					}
					
					final long relation = member.getRelation(player);
					final boolean isAutoAttackable = member.isAutoAttackable(player);
					final RelationCache oldrelation = member.getKnownRelations().get(player.getObjectId());
					if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
					{
						final RelationChanged rc = new RelationChanged();
						rc.addRelation(member, relation, isAutoAttackable);
						if (member.hasSummon())
						{
							final Summon pet = member.getPet();
							if (pet != null)
							{
								rc.addRelation(pet, relation, isAutoAttackable);
							}
							if (member.hasServitors())
							{
								member.getServitors().values().forEach(s -> rc.addRelation(s, relation, isAutoAttackable));
							}
						}
						player.sendPacket(rc);
						member.getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
					}
				});
			}
		}
	}
	
	/**
	 * Approve clan as defender for siege
	 * @param clanId The int of player's clan id
	 */
	public void approveSiegeDefenderClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		saveSiegeClan(ClanTable.getInstance().getClan(clanId), DEFENDER, true);
		loadSiegeClan();
	}
	
	/**
	 * @param object
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(WorldObject object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return (_isInProgress && (_castle.checkIfInZone(x, y, z))); // Castle zone during siege
	}
	
	/**
	 * Return true if clan is attacker
	 * @param clan The Clan of the player
	 */
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}
	
	/**
	 * Return true if clan is defender
	 * @param clan The Clan of the player
	 */
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return (getDefenderClan(clan) != null);
	}
	
	/**
	 * @param clan The Clan of the player
	 * @return true if clan is defender waiting approval
	 */
	public boolean checkIsDefenderWaiting(Clan clan)
	{
		return (getDefenderWaitingClan(clan) != null);
	}
	
	/** Clear all registered siege clans from database for castle */
	public void clearSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?"))
		{
			statement.setInt(1, _castle.getResidenceId());
			statement.execute();
			
			if (_castle.getOwnerId() > 0)
			{
				try (PreparedStatement delete = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?"))
				{
					delete.setInt(1, _castle.getOwnerId());
					delete.execute();
				}
			}
			
			getAttackerClans().clear();
			getDefenderClans().clear();
			_defenderWaitingClans.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: clearSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/** Clear all siege clans waiting for approval from database for castle */
	public void clearSiegeWaitingClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2"))
		{
			statement.setInt(1, _castle.getResidenceId());
			statement.execute();
			
			_defenderWaitingClans.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: clearSiegeWaitingClan(): " + e.getMessage(), e);
		}
	}
	
	/** Return list of Player registered as attacker in the zone. */
	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> result = new ArrayList<>();
		for (SiegeClan siegeclan : getAttackerClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if (clan != null)
			{
				for (Player member : clan.getOnlineMembers(0))
				{
					if (member.isInSiege())
					{
						result.add(member);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * @return list of Player in the zone.
	 */
	public List<Player> getPlayersInZone()
	{
		return _castle.getZone().getPlayersInside();
	}
	
	/**
	 * @return list of Player owning the castle in the zone.
	 */
	public List<Player> getOwnersInZone()
	{
		final List<Player> result = new ArrayList<>();
		for (SiegeClan siegeclan : getDefenderClans())
		{
			if (siegeclan.getClanId() == _castle.getOwnerId())
			{
				final Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
				if (clan != null)
				{
					for (Player member : clan.getOnlineMembers(0))
					{
						if (member.isInSiege())
						{
							result.add(member);
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * @return list of Player not registered as attacker or defender in the zone.
	 */
	public List<Player> getSpectatorsInZone()
	{
		final List<Player> result = new ArrayList<>();
		for (Player player : _castle.getZone().getPlayersInside())
		{
			if (!player.isInSiege())
			{
				result.add(player);
			}
		}
		return result;
	}
	
	/**
	 * Control Tower was killed
	 * @param ct
	 */
	public void killedCT(Npc ct)
	{
		_controlTowerCount = Math.max(_controlTowerCount - 1, 0);
	}
	
	/**
	 * Remove the flag that was killed
	 * @param flag
	 */
	public void killedFlag(Npc flag)
	{
		getAttackerClans().forEach(siegeClan -> siegeClan.removeFlag(flag));
	}
	
	/**
	 * Display list of registered clans
	 * @param player
	 */
	public void listRegisterClan(Player player)
	{
		player.sendPacket(new SiegeInfo(_castle, player));
	}
	
	/**
	 * Register clan as attacker
	 * @param player The Player of the player trying to register
	 */
	public void registerAttacker(Player player)
	{
		registerAttacker(player, false);
	}
	
	public void registerAttacker(Player player, boolean force)
	{
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		int allyId = 0;
		if (_castle.getOwnerId() != 0)
		{
			allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
		}
		if ((allyId != 0) && (clan.getAllyId() == allyId) && !force)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_AS_AN_ATTACKER_BECAUSE_YOU_ARE_IN_AN_ALLIANCE_WITH_THE_CASTLE_OWNING_CLAN);
			return;
		}
		
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
			}
			else
			{
				saveSiegeClan(clan, ATTACKER, false); // Save to database
			}
			return;
		}
		
		if (checkIfCanRegister(player, ATTACKER))
		{
			saveSiegeClan(clan, ATTACKER, false); // Save to database
		}
	}
	
	/**
	 * Register a clan as defender.
	 * @param player the player to register
	 */
	public void registerDefender(Player player)
	{
		registerDefender(player, false);
	}
	
	public void registerDefender(Player player, boolean force)
	{
		if (_castle.getOwnerId() <= 0)
		{
			player.sendMessage("You cannot register as a defender because " + _castle.getName() + " is owned by NPC.");
			return;
		}
		
		final Clan clan = player.getClan();
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
			}
			else
			{
				saveSiegeClan(clan, DEFENDER_NOT_APPROVED, false); // Save to database
			}
			return;
		}
		
		if (checkIfCanRegister(player, DEFENDER_NOT_APPROVED))
		{
			saveSiegeClan(clan, DEFENDER_NOT_APPROVED, false); // Save to database
		}
	}
	
	/**
	 * Remove clan from siege
	 * @param clanId The int of player's clan id
	 */
	public void removeSiegeClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?"))
		{
			statement.setInt(1, _castle.getResidenceId());
			statement.setInt(2, clanId);
			statement.execute();
			
			loadSiegeClan();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: removeSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove clan from siege
	 * @param clan clan being removed
	 */
	public void removeSiegeClan(Clan clan)
	{
		if ((clan == null) || (clan.getCastleId() == getCastle().getResidenceId()) || !SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
		{
			return;
		}
		removeSiegeClan(clan.getId());
	}
	
	/**
	 * Remove clan from siege
	 * @param player The Player of player/clan being removed
	 */
	public void removeSiegeClan(Player player)
	{
		removeSiegeClan(player.getClan());
	}
	
	/**
	 * Start the auto tasks.
	 */
	public void startAutoTask()
	{
		correctSiegeDateTime();
		
		LOGGER.info("Siege of " + _castle.getName() + ": " + _castle.getSiegeDate().getTime());
		loadSiegeClan();
		
		// Schedule siege auto start
		if (_scheduledStartSiegeTask != null)
		{
			_scheduledStartSiegeTask.cancel(false);
		}
		_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castle), 1000);
		startInfoTask();
	}
	
	private void startInfoTask()
	{
		if (_scheduledSiegeInfoTask != null)
		{
			_scheduledSiegeInfoTask.cancel(false);
		}
		
		_scheduledSiegeInfoTask = ThreadPool.schedule(() ->
		{
			for (Player player : World.getInstance().getPlayers())
			{
				SiegeManager.getInstance().sendSiegeInfo(player, _castle.getResidenceId());
			}
		}, Math.max(0, getSiegeDate().getTimeInMillis() - System.currentTimeMillis() - 3600000));
	}
	
	/**
	 * Teleport players
	 * @param teleportWho
	 * @param teleportWhere
	 */
	public void teleportPlayer(SiegeTeleportWhoType teleportWho, TeleportWhereType teleportWhere)
	{
		final List<Player> players;
		switch (teleportWho)
		{
			case Owner:
			{
				players = getOwnersInZone();
				break;
			}
			case NotOwner:
			{
				players = _castle.getZone().getPlayersInside();
				final Iterator<Player> it = players.iterator();
				while (it.hasNext())
				{
					final Player player = it.next();
					if ((player == null) || player.inObserverMode() || ((player.getClanId() > 0) && ((player.getClanId() == _castle.getOwnerId()) || (player.getClanIdMercenary() == _castle.getOwnerId()))))
					{
						it.remove();
					}
				}
				break;
			}
			case Attacker:
			{
				players = getAttackersInZone();
				break;
			}
			case Spectator:
			{
				players = getSpectatorsInZone();
				break;
			}
			default:
			{
				players = Collections.emptyList();
			}
		}
		
		for (Player player : players)
		{
			if (player.canOverrideCond(PlayerCondOverride.CASTLE_CONDITIONS) || player.isJailed())
			{
				continue;
			}
			player.teleToLocation(teleportWhere);
		}
	}
	
	/**
	 * Add clan as attacker
	 * @param clanId The int of clan's id
	 */
	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list
	}
	
	/**
	 * Add clan as defender
	 * @param clanId The int of clan's id
	 */
	private void addDefender(int clanId)
	{
		getDefenderClans().add(new SiegeClan(clanId, SiegeClanType.DEFENDER)); // Add registered defender to defender list
	}
	
	/**
	 * <p>
	 * Add clan as defender with the specified type
	 * </p>
	 * @param clanId The int of clan's id
	 * @param type the type of the clan
	 */
	private void addDefender(int clanId, SiegeClanType type)
	{
		getDefenderClans().add(new SiegeClan(clanId, type));
	}
	
	/**
	 * Add clan as defender waiting approval
	 * @param clanId The int of clan's id
	 */
	private void addDefenderWaiting(int clanId)
	{
		_defenderWaitingClans.add(new SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING)); // Add registered defender to defender list
	}
	
	/**
	 * @param player The Player of the player trying to register
	 * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
	 * @return true if the player can register.
	 */
	private boolean checkIfCanRegister(Player player, byte typeId)
	{
		final Clan clan = player.getClan();
		if (_isInProgress)
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
		}
		else if ((clan == null) || (clan.getLevel() < SiegeManager.getInstance().getSiegeClanMinLevel()))
		{
			player.sendPacket(SystemMessageId.ONLY_CLANS_OF_LEVEL_3_OR_ABOVE_MAY_REGISTER_FOR_A_CASTLE_SIEGE);
		}
		else if (clan.getId() == _castle.getOwnerId())
		{
			player.sendPacket(SystemMessageId.CASTLE_OWNING_CLANS_ARE_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
		}
		else if (clan.getCastleId() > 0)
		{
			player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
		}
		else if (SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_CASTLE_SIEGE);
		}
		else if (checkIfAlreadyRegisteredForSameDay(clan))
		{
			player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_CASTLE_SIEGE);
		}
		else if ((typeId == ATTACKER) && (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans()))
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
		}
		else if (((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROVED) || (typeId == OWNER)) && ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= SiegeManager.getInstance().getDefenderMaxClans()))
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_DEFENDER_SIDE);
		}
		// In Classic, only lvl 3-4 clans are able to participate in the Gludio Castle Siege.
		else if (((_castle.getResidenceId() == 1) && (clan.getLevel() >= 5)))
		{
			player.sendPacket(SystemMessageId.ONLY_LEVEL_3_4_CLANS_CAN_PARTICIPATE_IN_CASTLE_SIEGE);
		}
		else
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @param clan The Clan of the player trying to register
	 * @return true if the clan has already registered to a siege for the same day.
	 */
	public boolean checkIfAlreadyRegisteredForSameDay(Clan clan)
	{
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege == this)
			{
				continue;
			}
			if (siege.getSiegeDate().get(Calendar.DAY_OF_WEEK) == getSiegeDate().get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
				{
					return true;
				}
				if (siege.checkIsDefender(clan))
				{
					return true;
				}
				if (siege.checkIsDefenderWaiting(clan))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Return the correct siege date as Calendar.
	 */
	public void correctSiegeDateTime()
	{
		boolean corrected = false;
		if (getCastle().getSiegeDate().getTimeInMillis() < System.currentTimeMillis())
		{
			// Since siege has past reschedule it to the next one
			// This is usually caused by server being down
			corrected = true;
			setNextSiegeDate();
		}
		
		if (corrected)
		{
			saveSiegeDate();
		}
	}
	
	/** Load siege clans. */
	private void loadSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?"))
		{
			getAttackerClans().clear();
			getDefenderClans().clear();
			_defenderWaitingClans.clear();
			
			// Add castle owner as defender (add owner first so that they are on the top of the defender list)
			if (_castle.getOwnerId() > 0)
			{
				addDefender(_castle.getOwnerId(), SiegeClanType.OWNER);
			}
			
			statement.setInt(1, _castle.getResidenceId());
			try (ResultSet rs = statement.executeQuery())
			{
				int typeId;
				while (rs.next())
				{
					typeId = rs.getInt("type");
					if (typeId == DEFENDER)
					{
						addDefender(rs.getInt("clan_id"));
					}
					else if (typeId == ATTACKER)
					{
						addAttacker(rs.getInt("clan_id"));
					}
					else if (typeId == DEFENDER_NOT_APPROVED)
					{
						addDefenderWaiting(rs.getInt("clan_id"));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: loadSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/** Remove all spawned towers. */
	private void removeTowers()
	{
		for (FlameTower ct : _flameTowers)
		{
			ct.deleteMe();
		}
		
		for (ControlTower ct : _controlTowers)
		{
			ct.deleteMe();
		}
		
		for (Npc ct : _relic)
		{
			ct.deleteMe();
		}
		
		_flameTowers.clear();
		_controlTowers.clear();
	}
	
	/** Remove all flags. */
	private void removeFlags()
	{
		for (SiegeClan sc : getAttackerClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
		for (SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/** Remove flags from defenders. */
	private void removeDefenderFlags()
	{
		for (SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/** Save castle siege related to database. */
	private void saveCastleSiege()
	{
		setNextSiegeDate(); // Set the next set date for 2 weeks from now
		// Schedule Time registration end
		getTimeRegistrationOverDate().setTimeInMillis(System.currentTimeMillis());
		_castle.getTimeRegistrationOverDate().add(Calendar.DAY_OF_MONTH, 1);
		_castle.setTimeRegistrationOver(false);
		
		saveSiegeDate(); // Save the new date
		startAutoTask(); // Prepare auto start siege and end registration
	}
	
	/** Save siege date to database. */
	public void saveSiegeDate()
	{
		if (_scheduledStartSiegeTask != null)
		{
			_scheduledStartSiegeTask.cancel(true);
			_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castle), 1000);
		}
		startInfoTask();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET siegeDate = ?, regTimeEnd = ?, regTimeOver = ?  WHERE id = ?"))
		{
			statement.setLong(1, _castle.getSiegeDate().getTimeInMillis());
			statement.setLong(2, 0);
			statement.setString(3, "false");
			statement.setInt(4, _castle.getResidenceId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: saveSiegeDate(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Save registration to database.
	 * @param clan The Clan of player
	 * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
	 * @param isUpdateRegistration
	 */
	private void saveSiegeClan(Clan clan, byte typeId, boolean isUpdateRegistration)
	{
		if (clan.getCastleId() > 0)
		{
			return;
		}
		
		// Check if clan has mercenary.
		for (ClanMember clanMember : clan.getMembers())
		{
			if (clanMember.getPlayer() != null)
			{
				if (clanMember.getPlayer().isMercenary())
				{
					clanMember.getPlayer().setMercenary(false, clanMember.getPlayer().getClanIdMercenary());
				}
			}
			else
			{
				clan.removeMercenaryByPlayerId(clanMember.getObjectId());
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROVED) || (typeId == OWNER))
			{
				if ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= SiegeManager.getInstance().getDefenderMaxClans())
				{
					return;
				}
			}
			else if (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans())
			{
				return;
			}
			
			if (!isUpdateRegistration)
			{
				try (PreparedStatement statement = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) values (?,?,?,0)"))
				{
					statement.setInt(1, clan.getId());
					statement.setInt(2, _castle.getResidenceId());
					statement.setInt(3, typeId);
					statement.execute();
				}
			}
			else
			{
				try (PreparedStatement statement = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?"))
				{
					statement.setInt(1, typeId);
					statement.setInt(2, _castle.getResidenceId());
					statement.setInt(3, clan.getId());
					statement.execute();
				}
			}
			
			if ((typeId == DEFENDER) || (typeId == OWNER))
			{
				addDefender(clan.getId());
			}
			else if (typeId == ATTACKER)
			{
				addAttacker(clan.getId());
			}
			else if (typeId == DEFENDER_NOT_APPROVED)
			{
				addDefenderWaiting(clan.getId());
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: saveSiegeClan(Pledge clan, int typeId, boolean isUpdateRegistration): " + e.getMessage(), e);
		}
	}
	
	/** Set the date for the next siege. */
	private void setNextSiegeDate()
	{
		final SiegeScheduleDate holder = SiegeScheduleData.getInstance().getScheduleDateForCastleId(_castle.getResidenceId());
		if ((holder == null) || !holder.siegeEnabled())
		{
			return;
		}
		
		final Calendar calendar = _castle.getSiegeDate();
		if (calendar.getTimeInMillis() < System.currentTimeMillis())
		{
			calendar.setTimeInMillis(System.currentTimeMillis());
		}
		
		calendar.set(Calendar.DAY_OF_WEEK, holder.getDay());
		calendar.set(Calendar.HOUR_OF_DAY, holder.getHour());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		if (calendar.before(Calendar.getInstance()))
		{
			calendar.add(Calendar.WEEK_OF_YEAR, SiegeManager.getInstance().getSiegeCycle());
		}
		
		if (CastleManager.getInstance().getSiegeDates(calendar.getTimeInMillis()) < holder.getMaxConcurrent())
		{
			CastleManager.getInstance().registerSiegeDate(getCastle().getResidenceId(), calendar.getTimeInMillis());
			
			Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.S1_HAS_ANNOUNCED_THE_NEXT_CASTLE_SIEGE_TIME).addCastleId(_castle.getResidenceId()));
			
			// Allow registration for next siege
			_isRegistrationOver = false;
		}
		else
		{
			// Deny registration for next siege
			_isRegistrationOver = true;
		}
	}
	
	private void spawnRelic()
	{
		try
		{
			final TowerSpawn ts = SiegeManager.getInstance().getRelicTowers(getCastle().getResidenceId());
			final Spawn spawn = new Spawn(ts.getId());
			spawn.setLocation(ts.getLocation());
			final Npc npc = spawn.doSpawn(false);
			_relic.add(npc);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot spawn Relic! " + e);
		}
	}
	
	/**
	 * Spawn control tower.
	 */
	private void spawnControlTower()
	{
		try
		{
			for (TowerSpawn ts : SiegeManager.getInstance().getControlTowers(getCastle().getResidenceId()))
			{
				final Spawn spawn = new Spawn(ts.getId());
				spawn.setLocation(ts.getLocation());
				_controlTowers.add((ControlTower) spawn.doSpawn(false));
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot spawn control tower! " + e);
		}
		_controlTowerCount = _controlTowers.size();
	}
	
	/**
	 * Spawn flame tower.
	 */
	private void spawnFlameTower()
	{
		try
		{
			for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(getCastle().getResidenceId()))
			{
				final Spawn spawn = new Spawn(ts.getId());
				spawn.setLocation(ts.getLocation());
				final FlameTower tower = (FlameTower) spawn.doSpawn(false);
				tower.setUpgradeLevel(ts.getUpgradeLevel());
				tower.setZoneList(ts.getZoneList());
				_flameTowers.add(tower);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot spawn flame tower! " + e);
		}
	}
	
	/**
	 * Spawn siege guard.
	 */
	private void spawnSiegeGuard()
	{
		SiegeGuardManager.getInstance().spawnSiegeGuard(getCastle());
		
		// Register guard to the closest Control Tower
		// When CT dies, so do all the guards that it controls
		final Set<Spawn> spawned = SiegeGuardManager.getInstance().getSpawnedGuards(getCastle().getResidenceId());
		if (!spawned.isEmpty())
		{
			ControlTower closestCt;
			double distance;
			double distanceClosest = 0;
			for (Spawn spawn : spawned)
			{
				if (spawn == null)
				{
					continue;
				}
				
				closestCt = null;
				distanceClosest = Integer.MAX_VALUE;
				for (ControlTower ct : _controlTowers)
				{
					if (ct == null)
					{
						continue;
					}
					
					distance = ct.calculateDistance3D(spawn);
					if (distance < distanceClosest)
					{
						closestCt = ct;
						distanceClosest = distance;
					}
				}
				if (closestCt != null)
				{
					closestCt.registerGuard(spawn);
				}
			}
		}
	}
	
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		return getAttackerClan(clan.getId());
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		for (SiegeClan sc : getAttackerClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		return null;
	}
	
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		if (_isNormalSide)
		{
			return _attackerClans;
		}
		return _defenderClans;
	}
	
	public int getAttackerRespawnDelay()
	{
		return (SiegeManager.getInstance().getAttackerRespawnDelay());
	}
	
	public Castle getCastle()
	{
		return _castle;
	}
	
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		return getDefenderClan(clan.getId());
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		for (SiegeClan sc : getDefenderClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		return null;
	}
	
	@Override
	public Collection<SiegeClan> getDefenderClans()
	{
		if (_isNormalSide)
		{
			return _defenderClans;
		}
		return _attackerClans;
	}
	
	public SiegeClan getDefenderWaitingClan(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		return getDefenderWaitingClan(clan.getId());
	}
	
	public SiegeClan getDefenderWaitingClan(int clanId)
	{
		for (SiegeClan sc : _defenderWaitingClans)
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		return null;
	}
	
	public Collection<SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}
	
	public boolean isInProgress()
	{
		return _isInProgress;
	}
	
	public boolean isRegistrationOver()
	{
		return _isRegistrationOver;
	}
	
	public boolean isTimeRegistrationOver()
	{
		return _castle.isTimeRegistrationOver();
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		return _castle.getSiegeDate();
	}
	
	public Calendar getTimeRegistrationOverDate()
	{
		return _castle.getTimeRegistrationOverDate();
	}
	
	public void endTimeRegistration(boolean automatic)
	{
		_castle.setTimeRegistrationOver(true);
		if (!automatic)
		{
			saveSiegeDate();
		}
	}
	
	@Override
	public Set<Npc> getFlag(Clan clan)
	{
		if (clan != null)
		{
			final SiegeClan sc = getAttackerClan(clan);
			if (sc != null)
			{
				return sc.getFlag();
			}
		}
		return null;
	}
	
	public int getControlTowerCount()
	{
		return _controlTowerCount;
	}
	
	@Override
	public boolean giveFame()
	{
		return true;
	}
	
	@Override
	public int getFameFrequency()
	{
		return Config.CASTLE_ZONE_FAME_TASK_FREQUENCY;
	}
	
	@Override
	public int getFameAmount()
	{
		return Config.CASTLE_ZONE_FAME_AQUIRE_POINTS;
	}
	
	@Override
	public void updateSiege()
	{
	}
}
