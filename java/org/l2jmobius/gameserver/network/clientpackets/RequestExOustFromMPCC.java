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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * D0 0F 00 5A 00 77 00 65 00 72 00 67 00 00 00
 * @author -Wooden-
 */
public class RequestExOustFromMPCC extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player target = World.getInstance().getPlayer(_name);
		final Player player = getPlayer();
		final Party party = player.getParty();
		if ((target != null) && target.isInParty() && (party != null) && party.isInCommandChannel() && target.getParty().isInCommandChannel() && party.getCommandChannel().getLeader().equals(player) && party.getCommandChannel().equals(target.getParty().getCommandChannel()))
		{
			if (player.equals(target))
			{
				return;
			}
			
			target.getParty().getCommandChannel().removeParty(target.getParty());
			
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_ARE_DISMISSED_FROM_THE_COMMAND_CHANNEL);
			target.getParty().broadcastPacket(sm);
			
			// check if CC has not been canceled
			if (party.isInCommandChannel())
			{
				sm = new SystemMessage(SystemMessageId.C1_S_PARTY_IS_DISMISSED_FROM_THE_COMMAND_CHANNEL);
				sm.addString(target.getParty().getLeader().getName());
				party.getCommandChannel().broadcastPacket(sm);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
		}
	}
}
