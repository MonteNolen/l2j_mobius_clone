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
package org.l2jmobius.gameserver.model.item.enchant;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.type.CrystalType;

/**
 * @author Index
 */
public class RewardItemsOnFailure
{
	private final Map<CrystalType, Map<Integer, ItemChanceHolder>> _rewards = new EnumMap<>(CrystalType.class);
	private int _minEnchantLevel = Integer.MAX_VALUE;
	private int _maxEnchantLevel = Integer.MIN_VALUE;
	
	public RewardItemsOnFailure()
	{
	}
	
	public void addItemToHolder(int itemId, CrystalType grade, int enchantLevel, long count, double chance)
	{
		final ItemChanceHolder item = new ItemChanceHolder(itemId, chance, count);
		_rewards.computeIfAbsent(grade, _ -> new HashMap<>()).put(enchantLevel, item);
		_minEnchantLevel = Math.min(_minEnchantLevel, enchantLevel);
		_maxEnchantLevel = Math.max(_maxEnchantLevel, enchantLevel);
	}
	
	public ItemChanceHolder getRewardItem(CrystalType grade, int enchantLevel)
	{
		return _rewards.getOrDefault(grade, new HashMap<>()).getOrDefault(enchantLevel, null);
	}
	
	public boolean checkIfRewardUnavailable(CrystalType grade, int enchantLevel)
	{
		// reversed available
		if (_minEnchantLevel > enchantLevel)
		{
			return true;
		}
		
		if (_maxEnchantLevel < enchantLevel)
		{
			return true;
		}
		
		if (!_rewards.containsKey(grade))
		{
			return true;
		}
		
		return !_rewards.get(grade).containsKey(enchantLevel);
	}
	
	public int size()
	{
		int count = 0;
		for (Map<Integer, ItemChanceHolder> rewards : _rewards.values())
		{
			count += rewards.size();
		}
		return count;
	}
}
