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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.events.LetterCollectorManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author Index, Mobius
 */
public class ExLetterCollectorTakeReward extends ClientPacket
{
	private int _wordId;
	
	@Override
	protected void readImpl()
	{
		_wordId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		if (inventory == null)
		{
			return;
		}
		
		final LetterCollectorManager.LetterCollectorRewardHolder lcrh = LetterCollectorManager.getInstance().getRewards(_wordId);
		if (lcrh == null)
		{
			return;
		}
		
		for (ItemHolder needLetter : LetterCollectorManager.getInstance().getWord(_wordId))
		{
			if (inventory.getInventoryItemCount(needLetter.getId(), -1) < needLetter.getCount())
			{
				return;
			}
		}
		for (ItemHolder destroyLetter : LetterCollectorManager.getInstance().getWord(_wordId))
		{
			if (!player.destroyItemByItemId(ItemProcessType.FEE, destroyLetter.getId(), destroyLetter.getCount(), player, true))
			{
				return;
			}
		}
		
		final ItemChanceHolder rewardItem = getRandomReward(lcrh.getRewards(), lcrh.getChance());
		if (rewardItem == null)
		{
			player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
			return;
		}
		
		player.addItem(ItemProcessType.REWARD, rewardItem.getId(), rewardItem.getCount(), rewardItem.getEnchantmentLevel(), player, true);
	}
	
	private ItemChanceHolder getRandomReward(List<ItemChanceHolder> rewards, double holderChance)
	{
		final double chance = Rnd.get(holderChance);
		double itemChance = 0;
		for (ItemChanceHolder rewardItem : rewards)
		{
			itemChance += rewardItem.getChance();
			if (chance <= itemChance)
			{
				if (rewardItem.getId() == -1)
				{
					return null;
				}
				return rewardItem;
			}
		}
		return null;
	}
}