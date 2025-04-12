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
package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.chatbackground.ExChatBackgroundList;

/**
 * @author Liamxroy
 */
public class ChatBackground implements IItemHandler
{
	@Override
	public boolean useItem(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final Player player = playable.asPlayer();
		final PlayerVariables variables = player.getVariables();
		if (item.getId() == 100326) // First Lv. 94 Achievement Reward - Name Background
		{
			if (variables.getBoolean(PlayerVariables.CHAT_BACKGROUND_BLUE, false))
			{
				player.sendMessage("You already have blue ornament registered.");
				return false;
			}
			
			variables.set(PlayerVariables.CHAT_BACKGROUND_BLUE, true);
			player.sendMessage("Ornaments updated with blue background.");
		}
		else if (item.getId() == 100546) // Golden Name Background
		{
			if (variables.getBoolean(PlayerVariables.CHAT_BACKGROUND_YELLOW, false))
			{
				player.sendMessage("You already have yellow ornament registered.");
				return false;
			}
			
			variables.set(PlayerVariables.CHAT_BACKGROUND_YELLOW, true);
			player.sendMessage("Ornaments updated with yellow background.");
		}
		
		player.destroyItem(ItemProcessType.DESTROY, item.getObjectId(), 1, null, false);
		player.sendPacket(new ExChatBackgroundList(variables));
		return true;
	}
}
