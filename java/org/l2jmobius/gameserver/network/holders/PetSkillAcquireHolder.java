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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Berezkin Nikolay
 */
public class PetSkillAcquireHolder
{
	private final int _skillId;
	private final int _skillLevel;
	private final int _reqLvl;
	private final int _evolve;
	private final ItemHolder _item;
	
	public PetSkillAcquireHolder(int skillId, int skillLevel, int reqLvl, int evolve, ItemHolder item)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_reqLvl = reqLvl;
		_evolve = evolve;
		_item = item;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	public int getReqLvl()
	{
		return _reqLvl;
	}
	
	public int getEvolve()
	{
		return _evolve;
	}
	
	public ItemHolder getItem()
	{
		return _item;
	}
}
