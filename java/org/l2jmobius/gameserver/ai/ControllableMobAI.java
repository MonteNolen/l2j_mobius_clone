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
package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.MobGroup;
import org.l2jmobius.gameserver.model.MobGroupTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.ControllableMob;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * AI for controllable mobs
 * @author littlecrow
 */
public class ControllableMobAI extends AttackableAI
{
	public static final int AI_IDLE = 1;
	public static final int AI_NORMAL = 2;
	public static final int AI_FORCEATTACK = 3;
	public static final int AI_FOLLOW = 4;
	public static final int AI_CAST = 5;
	public static final int AI_ATTACK_GROUP = 6;
	
	private int _alternateAI;
	
	private boolean _isThinking; // to prevent thinking recursively
	private boolean _isNotMoving;
	
	private Creature _forcedTarget;
	private MobGroup _targetGroup;
	
	protected void thinkFollow()
	{
		final Attackable me = _actor.asAttackable();
		if (!LocationUtil.checkIfInRange(MobGroupTable.FOLLOW_RANGE, me, getForcedTarget(), true))
		{
			final int signX = Rnd.nextBoolean() ? -1 : 1;
			final int signY = Rnd.nextBoolean() ? -1 : 1;
			final int randX = Rnd.get(MobGroupTable.FOLLOW_RANGE);
			final int randY = Rnd.get(MobGroupTable.FOLLOW_RANGE);
			moveTo(getForcedTarget().getX() + (signX * randX), getForcedTarget().getY() + (signY * randY), getForcedTarget().getZ());
		}
	}
	
	@Override
	public void onActionThink()
	{
		if (_isThinking)
		{
			return;
		}
		
		setThinking(true);
		
		try
		{
			switch (_alternateAI)
			{
				case AI_IDLE:
				{
					if (getIntention() != Intention.ACTIVE)
					{
						setIntention(Intention.ACTIVE);
					}
					break;
				}
				case AI_FOLLOW:
				{
					thinkFollow();
					break;
				}
				case AI_CAST:
				{
					thinkCast();
					break;
				}
				case AI_FORCEATTACK:
				{
					thinkForceAttack();
					break;
				}
				case AI_ATTACK_GROUP:
				{
					thinkAttackGroup();
					break;
				}
				default:
				{
					if (getIntention() == Intention.ACTIVE)
					{
						thinkActive();
					}
					else if (getIntention() == Intention.ATTACK)
					{
						thinkAttack();
					}
					break;
				}
			}
		}
		finally
		{
			setThinking(false);
		}
	}
	
	@Override
	protected void thinkCast()
	{
		WorldObject target = _skill.getTarget(_actor, _forceUse, _dontMove, false);
		if ((target == null) || !target.isCreature() || target.asCreature().isAlikeDead())
		{
			target = _skill.getTarget(_actor, findNextRndTarget(), _forceUse, _dontMove, false);
		}
		
		if (target == null)
		{
			return;
		}
		
		setTarget(target);
		
		if (!_actor.isMuted())
		{
			int maxRange = 0;
			// check distant skills
			for (Skill sk : _actor.getAllSkills())
			{
				if (LocationUtil.checkIfInRange(sk.getCastRange(), _actor, target, true) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
				{
					_actor.doCast(sk);
					return;
				}
				maxRange = Math.max(maxRange, sk.getCastRange());
			}
			
			if (!_isNotMoving)
			{
				moveToPawn(target, maxRange);
			}
		}
	}
	
	protected void thinkAttackGroup()
	{
		final Creature target = getForcedTarget();
		if ((target == null) || target.isAlikeDead())
		{
			// try to get next group target
			setForcedTarget(findNextGroupTarget());
			clientStopMoving(null);
		}
		
		if (target == null)
		{
			return;
		}
		
		setTarget(target);
		// as a response, we put the target in a forcedattack mode
		final ControllableMob theTarget = (ControllableMob) target;
		final ControllableMobAI controllableMobAI = (ControllableMobAI) theTarget.getAI();
		controllableMobAI.forceAttack(_actor);
		
		final double distance = _actor.calculateDistance2D(target);
		final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
		int maxRange = range;
		if (!_actor.isMuted() && (distance > (range + 20)))
		{
			// check distant skills
			for (Skill sk : _actor.getAllSkills())
			{
				final int castRange = sk.getCastRange();
				if ((castRange >= distance) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
				{
					_actor.doCast(sk);
					return;
				}
				
				maxRange = Math.max(maxRange, castRange);
			}
			
			if (!_isNotMoving)
			{
				moveToPawn(target, range);
			}
			
			return;
		}
		_actor.doAutoAttack(target);
	}
	
	protected void thinkForceAttack()
	{
		if ((getForcedTarget() == null) || getForcedTarget().isAlikeDead())
		{
			clientStopMoving(null);
			setIntention(Intention.ACTIVE);
			setAlternateAI(AI_IDLE);
		}
		
		setTarget(getForcedTarget());
		final double distance = _actor.calculateDistance2D(getForcedTarget());
		final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + getForcedTarget().getTemplate().getCollisionRadius();
		int maxRange = range;
		if (!_actor.isMuted() && (distance > (range + 20)))
		{
			// check distant skills
			for (Skill sk : _actor.getAllSkills())
			{
				final int castRange = sk.getCastRange();
				if ((castRange >= distance) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
				{
					_actor.doCast(sk);
					return;
				}
				
				maxRange = Math.max(maxRange, castRange);
			}
			
			if (!_isNotMoving)
			{
				moveToPawn(getForcedTarget(), _actor.getPhysicalAttackRange()/* range */);
			}
			
			return;
		}
		
		_actor.doAutoAttack(getForcedTarget());
	}
	
	@Override
	protected void thinkAttack()
	{
		Creature target = getForcedTarget();
		if ((target == null) || target.isAlikeDead())
		{
			if (target != null)
			{
				// stop hating
				final Attackable npc = _actor.asAttackable();
				npc.stopHating(target);
			}
			
			setIntention(Intention.ACTIVE);
		}
		else
		{
			// notify aggression
			final Creature finalTarget = target;
			if (_actor.asNpc().getTemplate().getClans() != null)
			{
				World.getInstance().forEachVisibleObject(_actor, Npc.class, npc ->
				{
					if (!npc.isInMyClan(_actor.asNpc()))
					{
						return;
					}
					
					if (_actor.isInsideRadius3D(npc, npc.getTemplate().getClanHelpRange()))
					{
						npc.getAI().notifyAction(Action.AGGRESSION, finalTarget, 1);
					}
				});
			}
			
			setTarget(target);
			final double distance = _actor.calculateDistance2D(target);
			final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
			int maxRange = range;
			if (!_actor.isMuted() && (distance > (range + 20)))
			{
				// check distant skills
				for (Skill sk : _actor.getAllSkills())
				{
					final int castRange = sk.getCastRange();
					if ((castRange >= distance) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() > _actor.getStat().getMpConsume(sk)))
					{
						_actor.doCast(sk);
						return;
					}
					
					maxRange = Math.max(maxRange, castRange);
				}
				
				moveToPawn(target, range);
				return;
			}
			
			// Force mobs to attack anybody if confused.
			Creature hated;
			if (_actor.isConfused())
			{
				hated = findNextRndTarget();
			}
			else
			{
				hated = target;
			}
			
			if (hated == null)
			{
				setIntention(Intention.ACTIVE);
				return;
			}
			
			if (hated != target)
			{
				target = hated;
			}
			
			if (!_actor.isMuted() && (Rnd.get(5) == 3))
			{
				for (Skill sk : _actor.getAllSkills())
				{
					final int castRange = sk.getCastRange();
					if ((castRange >= distance) && !_actor.isSkillDisabled(sk) && (_actor.getCurrentMp() < _actor.getStat().getMpConsume(sk)))
					{
						_actor.doCast(sk);
						return;
					}
				}
			}
			
			_actor.doAutoAttack(target);
		}
	}
	
	@Override
	protected void thinkActive()
	{
		Creature hated;
		if (_actor.isConfused())
		{
			hated = findNextRndTarget();
		}
		else
		{
			final WorldObject target = _actor.getTarget();
			hated = (target != null) && target.isCreature() ? target.asCreature() : null;
		}
		
		if (hated != null)
		{
			_actor.setRunning();
			setIntention(Intention.ATTACK, hated);
		}
	}
	
	private boolean checkAutoAttackCondition(Creature target)
	{
		if ((target == null) || !_actor.isAttackable())
		{
			return false;
		}
		
		if (target.isNpc() || target.isDoor())
		{
			return false;
		}
		
		final Attackable me = _actor.asAttackable();
		if (target.isAlikeDead() || !me.isInsideRadius2D(target, me.getAggroRange()) || (Math.abs(_actor.getZ() - target.getZ()) > 100))
		{
			return false;
		}
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
		{
			return false;
		}
		
		// Spawn protection (only against mobs)
		if (target.isPlayer() && target.asPlayer().isSpawnProtected())
		{
			return false;
		}
		
		// Check if the target is a Playable and if the target isn't in silent move mode
		if (target.isPlayable() && target.asPlayable().isSilentMovingAffected())
		{
			return false;
		}
		
		if (target.isNpc())
		{
			return false;
		}
		
		return me.isAggressive();
	}
	
	private Creature findNextRndTarget()
	{
		final List<Creature> potentialTarget = new ArrayList<>();
		World.getInstance().forEachVisibleObject(_actor, Creature.class, target ->
		{
			if (LocationUtil.checkIfInShortRange(_actor.asAttackable().getAggroRange(), _actor, target, true) && checkAutoAttackCondition(target))
			{
				potentialTarget.add(target);
			}
		});
		
		return !potentialTarget.isEmpty() ? potentialTarget.get(Rnd.get(potentialTarget.size())) : null;
	}
	
	private ControllableMob findNextGroupTarget()
	{
		return getGroupTarget().getRandomMob();
	}
	
	public ControllableMobAI(ControllableMob controllableMob)
	{
		super(controllableMob);
		setAlternateAI(AI_IDLE);
	}
	
	public int getAlternateAI()
	{
		return _alternateAI;
	}
	
	public void setAlternateAI(int alternateAi)
	{
		_alternateAI = alternateAi;
	}
	
	public void forceAttack(Creature target)
	{
		setAlternateAI(AI_FORCEATTACK);
		setForcedTarget(target);
	}
	
	public void forceAttackGroup(MobGroup group)
	{
		setForcedTarget(null);
		setGroupTarget(group);
		setAlternateAI(AI_ATTACK_GROUP);
	}
	
	public void stop()
	{
		setAlternateAI(AI_IDLE);
		clientStopMoving(null);
	}
	
	public void move(int x, int y, int z)
	{
		moveTo(x, y, z);
	}
	
	public void follow(Creature target)
	{
		setAlternateAI(AI_FOLLOW);
		setForcedTarget(target);
	}
	
	public boolean isThinking()
	{
		return _isThinking;
	}
	
	public boolean isNotMoving()
	{
		return _isNotMoving;
	}
	
	public void setNotMoving(boolean isNotMoving)
	{
		_isNotMoving = isNotMoving;
	}
	
	public void setThinking(boolean isThinking)
	{
		_isThinking = isThinking;
	}
	
	private Creature getForcedTarget()
	{
		return _forcedTarget;
	}
	
	private MobGroup getGroupTarget()
	{
		return _targetGroup;
	}
	
	private void setForcedTarget(Creature forcedTarget)
	{
		_forcedTarget = forcedTarget;
	}
	
	private void setGroupTarget(MobGroup targetGroup)
	{
		_targetGroup = targetGroup;
	}
}
