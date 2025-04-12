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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCombination;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;

/**
 * @author CostyKiller
 */
public class RequestRelicsCombination extends ClientPacket
{
	private int _relicsUsedGrade;
	private int _relicsUsedCount;
	private final List<Integer> _ingredientIds = new LinkedList<>();
	
	@Override
	protected void readImpl()
	{
		_relicsUsedGrade = readInt();
		_relicsUsedCount = readInt();
		while (remaining() > 0)
		{
			_ingredientIds.add(readInt());
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
		
		final Collection<PlayerRelicData> storedRelics = player.getRelics();
		final List<Integer> unconfirmedRelics = new ArrayList<>();
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() == 1)) // Unconfirmed relics are set on summon to index 300.
			{
				unconfirmedRelics.add(relic.getRelicId());
			}
		}
		if (unconfirmedRelics.size() == Config.RELIC_UNCONFIRMED_LIST_LIMIT) // If you have 100 relics in your confirmation list, restrictions are applied to the relic summoning and compounding functions.
		{
			player.sendPacket(SystemMessageId.SUMMON_COMPOUND_IS_UNAVAILABLE_AS_YOU_HAVE_MORE_THAN_100_UNCONFIRMED_DOLLS);
			return;
		}
		
		if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
		{
			player.sendMessage("Compound Ingredients: " + _relicsUsedCount);
		}
		
		// Check ingredient relics.
		int i = 0;
		for (int ingredientId : _ingredientIds)
		{
			i++;
			PlayerRelicData ingredientRelic = null;
			for (PlayerRelicData relic : storedRelics)
			{
				if ((relic.getRelicId() == ingredientId) && (relic.getRelicIndex() < 300)) // Only relics with index 0 and can be compounded.
				{
					ingredientRelic = relic;
					break;
				}
			}
			if ((ingredientRelic != null) && (ingredientRelic.getRelicCount() > 0))
			{
				ingredientRelic.setRelicCount(ingredientRelic.getRelicCount() - 1);
				if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					player.sendMessage("Ingredient Relic " + i + " data updated, ID: " + ingredientRelic.getRelicId() + ", Count: " + ingredientRelic.getRelicCount());
				}
			}
		}
		
		// Store relics.
		player.sendPacket(new ExRelicsList(player)); // Update confirmed relic list relics count.
		player.sendPacket(new ExRelicsCombination(player, _relicsUsedGrade, _relicsUsedCount));
		player.storeRelics();
	}
}
