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
package org.l2jmobius.gameserver.data.holders;

/**
 * @author CostyKiller
 */
public class RelicDataHolder
{
	private final int _relicId;
	private final int _grade;
	private final int _skillId;
	private final int _enchantLevel;
	private final int _skillLevel;
	
	public RelicDataHolder(int relicId, int grade, int skillId, int enchantLevel, int skillLevel)
	{
		_relicId = relicId;
		_grade = grade;
		_skillId = skillId;
		_enchantLevel = enchantLevel;
		_skillLevel = skillLevel;
	}
	
	public int getRelicId()
	{
		return _relicId;
	}
	
	public int getGrade()
	{
		return _grade;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public int getSkillLevel()
	{
		return _skillLevel;
	}
}
