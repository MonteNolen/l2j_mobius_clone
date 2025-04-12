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
package org.l2jmobius.gameserver.network.clientpackets.relics;

import java.util.Collection;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionUpdate;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsExchangeList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpdateList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpgrade;

/**
 * @author CostyKiller
 */
public class RequestRelicsUpgrade extends ClientPacket
{
	private int _relicId;
	private int _relicLevel;
	private int _ingredient1Id = 0;
	private int _ingredient2Id = 0;
	private int _ingredient3Id = 0;
	private int _ingredient4Id = 0;
	private int _chance = 0;
	
	@Override
	protected void readImpl()
	{
		_relicId = readInt();
		_relicLevel = readInt();
		
		switch (readInt()) // Ingredient count.
		{
			case 1:
			{
				_ingredient1Id = readInt();
				_chance = Config.RELIC_ENHANCEMENT_CHANCE_1_INGREDIENT;
				break;
			}
			case 2:
			{
				_ingredient1Id = readInt();
				_ingredient2Id = readInt();
				_chance = Config.RELIC_ENHANCEMENT_CHANCE_2_INGREDIENTS;
				break;
			}
			case 3:
			{
				_ingredient1Id = readInt();
				_ingredient2Id = readInt();
				_ingredient3Id = readInt();
				_chance = Config.RELIC_ENHANCEMENT_CHANCE_3_INGREDIENTS;
				break;
			}
			case 4:
			{
				_ingredient1Id = readInt();
				_ingredient2Id = readInt();
				_ingredient3Id = readInt();
				_ingredient4Id = readInt();
				_chance = Config.RELIC_ENHANCEMENT_CHANCE_4_INGREDIENTS;
				break;
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final boolean success = Rnd.get(100) <= _chance; // chance to successful upgrade is based on ingredients count.
		final Collection<PlayerRelicData> storedRelics = player.getRelics();
		PlayerRelicData existingRelic = null;
		
		// Check if the relic with the same ID exists.
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicId() == _relicId) && (relic.getRelicIndex() < 300)) // Only relics with index 0 can be enchanted.
			{
				existingRelic = relic;
				break;
			}
		}
		
		if ((existingRelic != null) && (existingRelic.getRelicLevel() < 4))
		{
			// Increment the level of the existing relic if successful upgrade.
			existingRelic.setRelicLevel(success ? existingRelic.getRelicLevel() + 1 : existingRelic.getRelicLevel());
			_relicLevel = existingRelic.getRelicLevel();
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Relic Id: " + existingRelic.getRelicId() + " " + (success ? "Upgrade successful! Relic is now level: " : "Upgrade failed! Relic is still level: ") + _relicLevel);
			}
			player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), existingRelic.getRelicLevel(), existingRelic.getRelicCount())); // Update confirmed relic list with new relic.
			if (!player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
			{
				// Auto-Add to relic collections on summon.
				player.sendPacket(new ExRelicsCollectionUpdate(player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
			}
		}
		
		// Check relic ingredients.
		// First Relic Ingredient checks.
		PlayerRelicData ingredientRelic1 = null;
		for (PlayerRelicData relic : storedRelics)
		{
			if (relic.getRelicId() == _ingredient1Id)
			{
				ingredientRelic1 = relic;
				break;
			}
		}
		if ((ingredientRelic1 != null) && (ingredientRelic1.getRelicCount() > 0))
		{
			ingredientRelic1.setRelicCount(ingredientRelic1.getRelicCount() - 1);
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Ingredient Relic 1 data updated, ID: " + ingredientRelic1.getRelicId() + ", Count: " + ingredientRelic1.getRelicCount());
			}
		}
		
		// Second Relic Ingredient checks.
		PlayerRelicData ingredientRelic2 = null;
		for (PlayerRelicData relic : storedRelics)
		{
			if (relic.getRelicId() == _ingredient2Id)
			{
				ingredientRelic2 = relic;
				break;
			}
		}
		if ((ingredientRelic2 != null) && (ingredientRelic2.getRelicCount() > 0))
		{
			ingredientRelic2.setRelicCount(ingredientRelic2.getRelicCount() - 1);
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Ingredient Relic 2 data updated, ID: " + ingredientRelic2.getRelicId() + ", Count: " + ingredientRelic2.getRelicCount());
			}
		}
		
		// Third Relic Ingredient checks.
		PlayerRelicData ingredientRelic3 = null;
		for (PlayerRelicData relic : storedRelics)
		{
			if (relic.getRelicId() == _ingredient3Id)
			{
				ingredientRelic3 = relic;
				break;
			}
		}
		if ((ingredientRelic3 != null) && (ingredientRelic3.getRelicCount() > 0))
		{
			ingredientRelic3.setRelicCount(ingredientRelic3.getRelicCount() - 1);
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Ingredient Relic 3 data updated, ID: " + ingredientRelic3.getRelicId() + ", Count: " + ingredientRelic3.getRelicCount());
			}
		}
		
		// Fourth Relic Ingredient checks.
		PlayerRelicData ingredientRelic4 = null;
		for (PlayerRelicData relic : storedRelics)
		{
			if (relic.getRelicId() == _ingredient4Id)
			{
				ingredientRelic4 = relic;
				break;
			}
		}
		if ((ingredientRelic4 != null) && (ingredientRelic4.getRelicCount() > 0))
		{
			ingredientRelic4.setRelicCount(ingredientRelic4.getRelicCount() - 1);
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Ingredient Relic 4 data updated, ID: " + ingredientRelic4.getRelicId() + ", Count: " + ingredientRelic4.getRelicCount());
			}
		}
		
		// Store relics.
		player.storeRelics();
		
		player.sendPacket(new ExRelicsList(player)); // Update confirmed relic list relics count.
		player.sendPacket(new ExRelicsExchangeList(player)); // Update relic exchange/confirm list.
		player.sendPacket(new ExRelicsUpgrade(player, success, _relicId, _relicLevel));
	}
}
