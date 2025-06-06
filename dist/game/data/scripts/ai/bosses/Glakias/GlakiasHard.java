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
package ai.bosses.Glakias;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * @author Serenitty
 */
public class GlakiasHard extends AbstractNpcAI
{
	// NPCs
	private static final int GLAKIAS = 29138;
	private static final int GLAKIAS2 = 29139;
	// Skills
	private static final SkillHolder SUMMON_GLAKIAS = new SkillHolder(48373, 2);
	private static final SkillHolder ICE_STORM_LV_1 = new SkillHolder(48381, 1);
	private static final SkillHolder ICE_CHAIN_LV_1 = new SkillHolder(48374, 1);
	private static final SkillHolder ICE_STORM_LV_2 = new SkillHolder(48381, 2);
	private static final SkillHolder ICE_CHAIN_LV_2 = new SkillHolder(48374, 2);
	private static final SkillHolder EMPEROR_THUNDER_LV_2 = new SkillHolder(48378, 2);
	private static final SkillHolder EMPEROR_SMASH_LV_2 = new SkillHolder(48377, 2);
	private static final SkillHolder GLAKIAS_ENCHANCEMENT_LV_1 = new SkillHolder(48372, 1);
	private static final SkillHolder GLAKIAS_ENCHANCEMENT_LV_2 = new SkillHolder(48372, 2);
	
	public GlakiasHard()
	{
		addKillId(GLAKIAS);
		addAttackId(GLAKIAS);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.isAttackable())
		{
			if (npc.getId() == GLAKIAS)
			{
				manageGlakiasSkills(npc, attacker);
			}
			else if (npc.getId() == GLAKIAS2)
			{
				manageTwoGlakiasSkills(npc, attacker);
			}
		}
	}
	
	private void manageGlakiasSkills(Npc npc, Player player)
	{
		if (npc.isCastingNow(SkillCaster::isAnyNormalType) || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		SkillHolder skillToCast = null;
		final double currentHpPercentage = npc.getCurrentHp() / npc.getMaxHp();
		if (currentHpPercentage > 45)
		{
			if (getRandom(100) < 40)
			{
				final int randomSkill = getRandom(4);
				switch (randomSkill)
				{
					case 0:
					{
						skillToCast = EMPEROR_THUNDER_LV_2;
						break;
					}
					case 1:
					{
						skillToCast = EMPEROR_SMASH_LV_2;
						break;
					}
					case 2:
					{
						skillToCast = ICE_CHAIN_LV_1;
						break;
					}
					case 3:
					{
						skillToCast = ICE_STORM_LV_1;
						break;
					}
				}
			}
		}
		else if ((currentHpPercentage < 45) && (getRandom(100) < 40))
		{
			final int randomSkill = getRandom(4);
			switch (randomSkill)
			{
				case 0:
				{
					skillToCast = ICE_STORM_LV_2;
					break;
				}
				case 1:
				{
					skillToCast = ICE_CHAIN_LV_2;
					break;
				}
				case 2:
				{
					skillToCast = GLAKIAS_ENCHANCEMENT_LV_1;
					break;
				}
				case 3:
				{
					skillToCast = SUMMON_GLAKIAS;
					break;
				}
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	private void manageTwoGlakiasSkills(Npc npc, Player player)
	{
		if (npc.isCastingNow(SkillCaster::isAnyNormalType) || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		SkillHolder skillToCast = null;
		final double currentHpPercentage = npc.getCurrentHp() / npc.getMaxHp();
		if (currentHpPercentage > 45)
		{
			if (getRandom(100) < 40)
			{
				final int randomSkill = getRandom(4);
				switch (randomSkill)
				{
					case 0:
					{
						skillToCast = GLAKIAS_ENCHANCEMENT_LV_1;
						break;
					}
					case 1:
					{
						skillToCast = ICE_CHAIN_LV_1;
						break;
					}
					case 2:
					{
						skillToCast = EMPEROR_SMASH_LV_2;
						break;
					}
					case 3:
					{
						skillToCast = ICE_STORM_LV_1;
						break;
					}
				}
			}
		}
		else if ((currentHpPercentage < 45) && (getRandom(100) < 40))
		{
			final int randomSkill = getRandom(4);
			switch (randomSkill)
			{
				case 0:
				{
					skillToCast = ICE_STORM_LV_2;
					break;
				}
				case 1:
				{
					skillToCast = ICE_CHAIN_LV_2;
					break;
				}
				case 2:
				{
					skillToCast = GLAKIAS_ENCHANCEMENT_LV_2;
					break;
				}
				case 3:
				{
					skillToCast = SUMMON_GLAKIAS;
					break;
				}
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	public static void main(String[] args)
	{
		new GlakiasHard();
	}
}
