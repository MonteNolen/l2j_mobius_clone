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
package handlers.effecthandlers;

import java.util.Collection;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class AssistOwnerCast extends AbstractEffect
{
	private final int _summonId;
	private final int _castSkillId;
	private final SkillHolder _skill;
	private final boolean _onFinishCast;
	
	public AssistOwnerCast(StatSet params)
	{
		_summonId = params.getInt("summonId"); // Npc id
		_castSkillId = params.getInt("castSkillId");
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 0));
		_onFinishCast = params.getBoolean("onFinishCast", false);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0) || (_castSkillId == 0))
		{
			return;
		}
		
		if (_onFinishCast)
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onSkillUseEvent(event), this));
		}
		else
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_USE, (OnCreatureSkillUse event) -> onSkillUseEvent(event), this));
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (_onFinishCast)
		{
			effected.removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
		}
		else
		{
			effected.removeListenerIf(EventType.ON_CREATURE_SKILL_USE, listener -> listener.getOwner() == this);
		}
	}
	
	private void onSkillUseEvent(OnCreatureSkillFinishCast event)
	{
		final Creature caster = event.getCaster();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || (_castSkillId != event.getSkill().getId()) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = event.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		processSummonActions(caster, target, _skill.getSkill());
	}
	
	private void onSkillUseEvent(OnCreatureSkillUse event)
	{
		final Creature caster = event.getCaster();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || (_castSkillId != event.getSkill().getId()) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = caster.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		processSummonActions(caster, target, _skill.getSkill());
	}
	
	private void processSummonActions(Creature caster, WorldObject target, Skill skill)
	{
		final Collection<Summon> servitors = caster.getServitors().values();
		if (!servitors.isEmpty())
		{
			servitors.forEach(servitor ->
			{
				if ((_summonId == servitor.getId()) && !servitor.isDisabled())
				{
					servitor.setTarget(target);
					if (_skill.getSkill().isBad())
					{
						servitor.getAI().setIntention(Intention.ATTACK, target);
					}
					servitor.doCast(skill);
				}
			});
		}
		
		final Collection<Npc> summonedNpcs = caster.getSummonedNpcs();
		if (!summonedNpcs.isEmpty())
		{
			summonedNpcs.forEach(summon ->
			{
				if ((_summonId == summon.getId()) && !summon.isDisabled())
				{
					summon.setTarget(target);
					if (_skill.getSkill().isBad())
					{
						summon.getAI().setIntention(Intention.ATTACK, target);
						summon.doAutoAttack((Creature) target);
					}
					summon.doCast(skill);
				}
			});
		}
	}
}
