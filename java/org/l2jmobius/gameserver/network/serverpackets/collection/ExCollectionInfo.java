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
package org.l2jmobius.gameserver.network.serverpackets.collection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExCollectionInfo extends ServerPacket
{
	final Player _player;
	final int _category;
	final Set<Integer> _collectionIds = new HashSet<>();
	final List<Integer> _favoriteIds;
	final List<CollectionHolder> _collectionHolders = new LinkedList<>();
	
	public ExCollectionInfo(Player player, int category)
	{
		_player = player;
		_category = category;
		for (PlayerCollectionData collection : player.getCollections())
		{
			if (CollectionData.getInstance().getCollection(collection.getCollectionId()).getCategory() == category)
			{
				_collectionIds.add(collection.getCollectionId());
			}
		}
		_favoriteIds = player.getCollectionFavorites();
		
		for (int id : _collectionIds)
		{
			final CollectionHolder holder = new CollectionHolder(id);
			for (PlayerCollectionData collection : player.getCollections())
			{
				if (collection.getCollectionId() == id)
				{
					holder.addCollectionData(collection, CollectionData.getInstance().getCollection(id).getItems().get(collection.getIndex()).getEnchantLevel());
				}
			}
			_collectionHolders.add(holder);
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_COLLECTION_INFO.writeId(this, buffer);
		
		// Write collectionHolders data
		buffer.writeInt(_collectionHolders.size()); // size
		for (CollectionHolder holder : _collectionHolders)
		{
			final List<CollectionDataHolder> collectionDataList = holder.getCollectionData();
			buffer.writeInt(collectionDataList.size());
			for (CollectionDataHolder dataHolder : collectionDataList)
			{
				final PlayerCollectionData data = dataHolder.getCollectionData();
				buffer.writeByte(data.getIndex());
				buffer.writeInt(data.getItemId());
				buffer.writeByte(dataHolder.getEnchantLevel()); // enchant level
				buffer.writeByte(0); // bless
				buffer.writeByte(0); // bless Condition
				buffer.writeInt(1); // amount
			}
			buffer.writeShort(holder.getCollectionId());
		}
		
		// favoriteList
		buffer.writeInt(_favoriteIds.size());
		for (int id : _favoriteIds)
		{
			buffer.writeShort(id);
		}
		
		// rewardList
		buffer.writeInt(0);
		
		buffer.writeByte(_category);
		buffer.writeShort(0);
	}
	
	private class CollectionHolder
	{
		private final int _collectionId;
		private final List<CollectionDataHolder> _collectionData;
		
		public CollectionHolder(int collectionId)
		{
			_collectionId = collectionId;
			_collectionData = new LinkedList<>();
		}
		
		public int getCollectionId()
		{
			return _collectionId;
		}
		
		public List<CollectionDataHolder> getCollectionData()
		{
			return _collectionData;
		}
		
		public void addCollectionData(PlayerCollectionData data, int enchantLevel)
		{
			_collectionData.add(new CollectionDataHolder(data, enchantLevel));
		}
	}
	
	private class CollectionDataHolder
	{
		private final PlayerCollectionData _collectionData;
		private final int _enchantLevel;
		
		public CollectionDataHolder(PlayerCollectionData collectionData, int enchantLevel)
		{
			_collectionData = collectionData;
			_enchantLevel = enchantLevel;
		}
		
		public PlayerCollectionData getCollectionData()
		{
			return _collectionData;
		}
		
		public int getEnchantLevel()
		{
			return _enchantLevel;
		}
	}
}
