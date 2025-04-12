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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillEnchantData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.AbnormalStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExPetSkillList;

/**
 * @author Mobius
 */
public class ReplaceSkillBySkill extends AbstractEffect
{
	private final SkillHolder _existingSkill;
	private final SkillHolder _replacementSkill;
	
	public ReplaceSkillBySkill(StatSet params)
	{
		_existingSkill = new SkillHolder(params.getInt("existingSkillId"), params.getInt("existingSkillLevel", -1));
		_replacementSkill = new SkillHolder(params.getInt("replacementSkillId"), params.getInt("replacementSkillLevel", -1));
		
		SkillEnchantData.getInstance().addReplacedSkillEnchant(_existingSkill.getSkillId(), _replacementSkill.getSkillId());
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayable() && (!effected.isTransformed() || effected.hasAbnormalType(AbnormalType.KAMAEL_TRANSFORM));
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Playable playable = effected.asPlayable();
		final Skill knownSkill = playable.getKnownSkill(_existingSkill.getSkillId());
		if ((knownSkill == null) || (knownSkill.getLevel() < _existingSkill.getSkillLevel()))
		{
			return;
		}
		
		final Skill addedSkill = SkillData.getInstance().getSkill(_replacementSkill.getSkillId(), _replacementSkill.getSkillLevel() < 1 ? knownSkill.getLevel() : _replacementSkill.getSkillLevel(), knownSkill.getSubLevel());
		if (playable.isPlayer())
		{
			final Player player = effected.asPlayer();
			player.addSkill(addedSkill, false);
			player.addReplacedSkill(_existingSkill.getSkillId(), _replacementSkill.getSkillId());
			for (Shortcut shortcut : player.getAllShortcuts())
			{
				if (shortcut.isAutoUse() && (shortcut.getType() == ShortcutType.SKILL) && (shortcut.getId() == knownSkill.getId()))
				{
					if (knownSkill.isBad())
					{
						if (player.getAutoUseSettings().getAutoSkills().contains(knownSkill.getId()))
						{
							player.getAutoUseSettings().getAutoSkills().add(addedSkill.getId());
							player.getAutoUseSettings().getAutoSkills().remove(Integer.valueOf(knownSkill.getId()));
						}
					}
					else if (player.getAutoUseSettings().getAutoBuffs().contains(knownSkill.getId()))
					{
						player.getAutoUseSettings().getAutoBuffs().add(addedSkill.getId());
						player.getAutoUseSettings().getAutoBuffs().remove(knownSkill.getId());
					}
				}
			}
			
			// Replace continuous effects.
			if (knownSkill.isContinuous() && player.isAffectedBySkill(knownSkill.getId()))
			{
				int abnormalTime = 0;
				for (BuffInfo info : player.getEffectList().getEffects())
				{
					if (info.getSkill().getId() == knownSkill.getId())
					{
						abnormalTime = info.getAbnormalTime();
						break;
					}
				}
				
				if (abnormalTime > 2000)
				{
					addedSkill.applyEffects(player, player);
					final AbnormalStatusUpdate asu = new AbnormalStatusUpdate();
					for (BuffInfo info : player.getEffectList().getEffects())
					{
						if (info.getSkill().getId() == addedSkill.getId())
						{
							info.resetAbnormalTime(abnormalTime);
							asu.addSkill(info);
						}
					}
					player.sendPacket(asu);
				}
			}
			
			player.removeSkill(knownSkill, false);
			player.sendSkillList();
		}
		else // Not player.
		{
			playable.addSkill(addedSkill);
			playable.removeSkill(knownSkill, false);
			if (playable.isPet())
			{
				final Pet pet = playable.asPet();
				pet.sendPacket(new ExPetSkillList(false, pet));
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Playable playable = effected.asPlayable();
		final int existingSkillId = _existingSkill.getSkillId();
		if (playable.getReplacementSkill(existingSkillId) == existingSkillId)
		{
			return;
		}
		
		final Skill knownSkill = playable.getKnownSkill(_replacementSkill.getSkillId());
		if (knownSkill == null)
		{
			return;
		}
		
		final Skill addedSkill = SkillData.getInstance().getSkill(existingSkillId, knownSkill.getLevel(), knownSkill.getSubLevel());
		if (playable.isPlayer())
		{
			final Player player = effected.asPlayer();
			player.addSkill(addedSkill, knownSkill.getLevel() != _existingSkill.getSkillLevel());
			player.removeReplacedSkill(existingSkillId);
			for (Shortcut shortcut : player.getAllShortcuts())
			{
				if (shortcut.isAutoUse() && (shortcut.getType() == ShortcutType.SKILL) && (shortcut.getId() == addedSkill.getId()))
				{
					if (knownSkill.isBad())
					{
						if (player.getAutoUseSettings().getAutoSkills().contains(knownSkill.getId()))
						{
							player.getAutoUseSettings().getAutoSkills().add(addedSkill.getId());
							player.getAutoUseSettings().getAutoSkills().remove(Integer.valueOf(knownSkill.getId()));
						}
					}
					else if (player.getAutoUseSettings().getAutoBuffs().contains(knownSkill.getId()))
					{
						player.getAutoUseSettings().getAutoBuffs().add(addedSkill.getId());
						player.getAutoUseSettings().getAutoBuffs().remove(knownSkill.getId());
					}
				}
			}
			
			// Replace continuous effects.
			if (knownSkill.isContinuous() && player.isAffectedBySkill(knownSkill.getId()))
			{
				int abnormalTime = 0;
				for (BuffInfo info : player.getEffectList().getEffects())
				{
					if (info.getSkill().getId() == knownSkill.getId())
					{
						abnormalTime = info.getAbnormalTime();
						break;
					}
				}
				
				if (abnormalTime > 2000)
				{
					addedSkill.applyEffects(player, player);
					final AbnormalStatusUpdate asu = new AbnormalStatusUpdate();
					for (BuffInfo info : player.getEffectList().getEffects())
					{
						if (info.getSkill().getId() == addedSkill.getId())
						{
							info.resetAbnormalTime(abnormalTime);
							asu.addSkill(info);
						}
					}
					player.sendPacket(asu);
				}
			}
			
			player.removeSkill(knownSkill, false);
			player.sendSkillList();
		}
		else // Not player.
		{
			playable.addSkill(addedSkill);
			playable.removeSkill(knownSkill, false);
			if (playable.isPet())
			{
				final Pet pet = playable.asPet();
				pet.sendPacket(new ExPetSkillList(false, pet));
			}
		}
	}
}
