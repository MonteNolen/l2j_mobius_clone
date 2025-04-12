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
package org.l2jmobius.gameserver.network.clientpackets.compound;

import org.l2jmobius.gameserver.data.xml.CombinationItemsData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.CompoundRequest;
import org.l2jmobius.gameserver.model.item.combination.CombinationItem;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantOneFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantTwoFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantTwoOK;

/**
 * @author Mobius
 */
public class RequestNewEnchantPushTwo extends ClientPacket
{
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_IN_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isProcessingRequest())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			player.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		final CompoundRequest request = player.getRequest(CompoundRequest.class);
		if ((request == null) || request.isProcessing())
		{
			player.sendPacket(ExEnchantTwoFail.STATIC_PACKET);
			return;
		}
		
		// Make sure player owns this item.
		request.setItemTwo(_objectId);
		final Item itemOne = request.getItemOne();
		final Item itemTwo = request.getItemTwo();
		if ((itemOne == null) || (itemTwo == null))
		{
			player.sendPacket(ExEnchantTwoFail.STATIC_PACKET);
			return;
		}
		
		// Lets prevent using same item twice. Also stackable item check.
		if ((itemOne.getObjectId() == itemTwo.getObjectId()) && (!itemOne.isStackable() || (player.getInventory().getInventoryItemCount(itemOne.getTemplate().getId(), -1) < 2)))
		{
			player.sendPacket(ExEnchantTwoFail.STATIC_PACKET);
			return;
		}
		
		final CombinationItem combinationItem = CombinationItemsData.getInstance().getItemsBySlots(itemOne.getId(), itemOne.getEnchantLevel(), itemTwo.getId(), itemTwo.getEnchantLevel());
		
		// Not implemented or not able to merge!
		if (combinationItem == null)
		{
			player.sendPacket(ExEnchantTwoFail.STATIC_PACKET);
			return;
		}
		
		player.sendPacket(ExEnchantTwoOK.STATIC_PACKET);
	}
}
