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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.player.ClassInfoHolder;

/**
 * Loads the the list of classes and it's info.
 * @author Zoey76
 */
public class ClassListData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClassListData.class.getName());
	
	private final Map<PlayerClass, ClassInfoHolder> _classData = new ConcurrentHashMap<>();
	
	protected ClassListData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_classData.clear();
		parseDatapackFile("data/stats/chars/classList.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classData.size() + " class data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		Node attr;
		PlayerClass classId;
		String className;
		PlayerClass parentClassId;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					attrs = d.getAttributes();
					if ("class".equals(d.getNodeName()))
					{
						attr = attrs.getNamedItem("classId");
						classId = PlayerClass.getPlayerClass(parseInteger(attr));
						if (classId == null)
						{
							System.out.println(parseInteger(attr));
						}
						attr = attrs.getNamedItem("name");
						className = attr.getNodeValue();
						attr = attrs.getNamedItem("parentClassId");
						parentClassId = (attr != null) ? PlayerClass.getPlayerClass(parseInteger(attr)) : null;
						_classData.put(classId, new ClassInfoHolder(classId, className, parentClassId));
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the complete list of classes.
	 * @return a map containing all class data, where each entry maps a {@link PlayerClass} to its corresponding {@link ClassInfoHolder}
	 */
	public Map<PlayerClass, ClassInfoHolder> getClassList()
	{
		return _classData;
	}
	
	/**
	 * Retrieves information for a specific class by its {@link PlayerClass}.
	 * @param classId the class ID to look up
	 * @return the {@link ClassInfoHolder} associated with the specified {@code classId}, or {@code null} if not found
	 */
	public ClassInfoHolder getClass(PlayerClass classId)
	{
		return _classData.get(classId);
	}
	
	/**
	 * Retrieves information for a specific class by its ID as an integer.
	 * @param classId the class ID as an integer
	 * @return the {@link ClassInfoHolder} associated with the specified {@code classId}, or {@code null} if not found
	 */
	public ClassInfoHolder getClass(int classId)
	{
		final PlayerClass id = PlayerClass.getPlayerClass(classId);
		return (id != null) ? _classData.get(id) : null;
	}
	
	public static ClassListData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassListData INSTANCE = new ClassListData();
	}
}
