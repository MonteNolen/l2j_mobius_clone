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
package org.l2jmobius.gameserver.model.options;

import java.util.logging.Logger;

/**
 * @author Pere, Mobius
 */
public class Variation
{
	private static final Logger LOGGER = Logger.getLogger(Variation.class.getSimpleName());
	
	private final int _mineralId;
	private final OptionDataGroup[] _effects = new OptionDataGroup[2];
	private int _itemGroup = -1;
	
	public Variation(int mineralId, int itemGroup)
	{
		_mineralId = mineralId;
		_itemGroup = itemGroup;
	}
	
	public int getMineralId()
	{
		return _mineralId;
	}
	
	public int getItemGroup()
	{
		return _itemGroup;
	}
	
	public void setEffectGroup(int order, OptionDataGroup group)
	{
		_effects[order] = group;
	}
	
	public Options getRandomEffect(int order, int targetItemId)
	{
		if (_effects == null)
		{
			LOGGER.warning("Null effect: for mineral " + _mineralId + ", order " + order);
			return null;
		}
		if (_effects[order] == null)
		{
			return null;
		}
		return _effects[order].getRandomEffect(targetItemId);
	}
}