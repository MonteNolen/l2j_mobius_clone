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
package org.l2jmobius.gameserver.network.serverpackets.quest;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Magik
 */
public class ExQuestUI extends ServerPacket
{
	private final Player _player;
	private final Collection<QuestState> _allQuestStates;
	private int _activeQuestCount = 0;
	
	public ExQuestUI(Player player)
	{
		_player = player;
		_allQuestStates = player.getAllQuestStates();
		for (QuestState questState : _allQuestStates)
		{
			if (questState.isStarted() && !questState.isCompleted())
			{
				_activeQuestCount++;
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_player == null)
		{
			return;
		}
		
		ServerPackets.EX_QUEST_UI.writeId(this, buffer);
		if (!_allQuestStates.isEmpty())
		{
			buffer.writeInt(_allQuestStates.size());
			for (QuestState questState : _allQuestStates)
			{
				buffer.writeInt(questState.getQuest().getId());
				buffer.writeInt(questState.getCount());
				buffer.writeByte(questState.getState());
			}
			buffer.writeInt(_activeQuestCount);
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
	}
}
