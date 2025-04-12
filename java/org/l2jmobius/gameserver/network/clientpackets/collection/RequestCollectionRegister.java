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
package org.l2jmobius.gameserver.network.clientpackets.collection;

import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionComplete;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionRegister;

/**
 * @author Berezkin Nikolay, Mobius
 */
public class RequestCollectionRegister extends ClientPacket
{
	private int _collectionId;
	private int _index;
	private int _itemObjId;
	
	@Override
	protected void readImpl()
	{
		_collectionId = readShort();
		_index = readInt();
		_itemObjId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_itemObjId);
		if (item == null)
		{
			player.sendMessage("Item not found.");
			return;
		}
		
		final CollectionDataHolder collection = CollectionData.getInstance().getCollection(_collectionId);
		if (collection == null)
		{
			player.sendMessage("Could not find collection.");
			return;
		}
		
		long count = 0;
		for (ItemEnchantHolder data : collection.getItems())
		{
			if ((data.getId() == item.getId()) && ((data.getEnchantLevel() == 0) || (data.getEnchantLevel() == item.getEnchantLevel())))
			{
				count = data.getCount();
				break;
			}
		}
		if ((count == 0) || (item.getCount() < count) || item.isEquipped())
		{
			player.sendMessage("Incorrect item count.");
			return;
		}
		
		PlayerCollectionData currentColl = null;
		for (PlayerCollectionData coll : player.getCollections())
		{
			if (coll.getCollectionId() == _collectionId)
			{
				currentColl = coll;
				break;
			}
		}
		
		if ((currentColl != null) && (currentColl.getIndex() == _index))
		{
			player.sendPacket(new ExCollectionRegister(false, _collectionId, _index, new ItemEnchantHolder(item.getId(), count, item.getEnchantLevel())));
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_ADDED_TO_YOUR_COLLECTION);
			player.sendPacket(new ConfirmDlg("Collection already registered;"));
			return;
		}
		
		player.destroyItem(ItemProcessType.FEE, item, count, player, true);
		
		player.sendPacket(new ExCollectionRegister(true, _collectionId, _index, new ItemEnchantHolder(item.getId(), count, item.getEnchantLevel())));
		
		player.getCollections().add(new PlayerCollectionData(_collectionId, item.getId(), _index));
		
		int completeCount = 0;
		for (PlayerCollectionData coll : player.getCollections())
		{
			if (coll.getCollectionId() == _collectionId)
			{
				completeCount++;
			}
		}
		if (completeCount == collection.getCompleteCount())
		{
			player.sendPacket(new ExCollectionComplete(_collectionId));
			
			// TODO: CollectionData.getInstance().getCollection(_collectionId).getName()
			player.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETED).addString(""));
			
			// Apply collection option if all requirements are met.
			final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
			if (options != null)
			{
				options.apply(player);
			}
		}
	}
}
