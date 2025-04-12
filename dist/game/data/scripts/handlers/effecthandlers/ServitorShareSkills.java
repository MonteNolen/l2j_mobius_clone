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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExPetSkillList;

/**
 * @author Geremy
 */
public class ServitorShareSkills extends AbstractEffect
{
	public static final int SERVITOR_SHARE_SKILL_ID = 1557;
	public static final int POWERFUL_SERVITOR_SHARE_SKILL_ID = 45054;
	// For Powerful servitor share (45054).
	public static final int[] SERVITOR_SHARE_PASSIVE_SKILLS =
	{
		50189,
		50468,
		50190,
		50353,
		50446,
		50444,
		50555,
		50445,
		50449,
		50448,
		50447,
		50450
	};
	
	public ServitorShareSkills(StatSet params)
	{
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			if ((player.getClient().getConnectionState() != ConnectionState.IN_GAME) || (player.getClient() == null))
			{
				// ThreadPool.schedule(() -> onStart(effector, effected, skill, item), 1000);
				return;
			}
			
			if (!effected.hasServitors())
			{
				return;
			}
			
			final Collection<Summon> summons = player.getServitors().values();
			for (int i = 0; i < SERVITOR_SHARE_PASSIVE_SKILLS.length; i++)
			{
				int passiveSkillId = SERVITOR_SHARE_PASSIVE_SKILLS[i];
				BuffInfo passiveSkillEffect = player.getEffectList().getBuffInfoBySkillId(passiveSkillId);
				if (passiveSkillEffect != null)
				{
					for (Summon s : summons)
					{
						s.addSkill(passiveSkillEffect.getSkill());
						s.broadcastInfo();
						if (s.isPet())
						{
							player.sendPacket(new ExPetSkillList(true, s.asPet()));
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		if (!effected.hasServitors())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final Collection<Summon> summons = player.getServitors().values();
		for (int i = 0; i < SERVITOR_SHARE_PASSIVE_SKILLS.length; i++)
		{
			final int passiveSkillId = SERVITOR_SHARE_PASSIVE_SKILLS[i];
			for (Summon s : summons)
			{
				final BuffInfo passiveSkillEffect = s.getEffectList().getBuffInfoBySkillId(passiveSkillId);
				if (passiveSkillEffect != null)
				{
					s.removeSkill(passiveSkillEffect.getSkill(), true);
					s.broadcastInfo();
					if (s.isPet())
					{
						player.sendPacket(new ExPetSkillList(true, s.asPet()));
					}
				}
			}
		}
	}
}
