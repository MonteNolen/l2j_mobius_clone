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
package handlers.skillconditionhandlers;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.skill.ISkillCondition;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillConditionAffectType;

/**
 * @author UnAfraid
 */
public class OpCheckClassSkillCondition implements ISkillCondition
{
	private final PlayerClass _classId;
	private final SkillConditionAffectType _affectType;
	private final boolean _isWithin;
	
	public OpCheckClassSkillCondition(StatSet params)
	{
		_classId = params.getEnum("classId", PlayerClass.class);
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
		_isWithin = params.getBoolean("isWithin");
	}
	
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return caster.isPlayer() && (_isWithin == (_classId == caster.asPlayer().getPlayerClass()));
			}
			case TARGET:
			{
				if ((target != null) && !target.isPlayer())
				{
					return _isWithin == (_classId == target.asPlayer().getPlayerClass());
				}
				break;
			}
		}
		return false;
	}
}
