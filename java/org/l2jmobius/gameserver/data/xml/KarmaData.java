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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.IXmlReader;

/**
 * @author UnAfraid
 */
public class KarmaData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(KarmaData.class.getName());
	
	private final Map<Integer, Double> _karmaTable = new HashMap<>();
	
	public KarmaData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_karmaTable.clear();
		parseDatapackFile("data/stats/chars/pcKarmaIncrease.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _karmaTable.size() + " karma modifiers.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("pcKarmaIncrease".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("increase".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int level = parseInteger(attrs, "lvl");
						if (level >= Config.PLAYER_MAXIMUM_LEVEL)
						{
							break;
						}
						_karmaTable.put(level, parseDouble(attrs, "val"));
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the karma multiplier for a given level.<br>
	 * This multiplier is used to calculate the amount of karma lost upon death for players at the specified level.
	 * @param level the level for which the karma multiplier is requested
	 * @return the {@code double} multiplier associated with the specified level, used to calculate karma loss
	 */
	public double getMultiplier(int level)
	{
		return _karmaTable.get(level);
	}
	
	public static KarmaData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final KarmaData INSTANCE = new KarmaData();
	}
}
