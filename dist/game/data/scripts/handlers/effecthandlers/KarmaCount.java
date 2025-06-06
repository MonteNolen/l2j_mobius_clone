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
package handlers.effecthandlers;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Item Effect: Decreases/resets karma count.
 * @author Nik
 */
public class KarmaCount extends AbstractEffect
{
	private final int _amount;
	private final int _mode;
	
	public KarmaCount(StatSet params)
	{
		_amount = params.getInt("amount", 0);
		switch (params.getString("mode", "DIFF"))
		{
			case "DIFF":
			{
				_mode = 0;
				break;
			}
			case "RESET":
			{
				_mode = 1;
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Mode should be DIFF or RESET skill id:" + params.getInt("id"));
			}
		}
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		// Check if player has no karma.
		if (player.getReputation() >= 0)
		{
			return;
		}
		
		switch (_mode)
		{
			case 0: // diff
			{
				final int newReputation = Math.min(player.getReputation() + _amount, 0);
				player.setReputation(newReputation);
				break;
			}
			case 1: // reset
			{
				player.setReputation(0);
			}
		}
	}
}
