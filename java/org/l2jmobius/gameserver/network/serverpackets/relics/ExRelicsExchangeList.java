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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsExchangeList extends ServerPacket
{
	private final Player _player;
	
	public ExRelicsExchangeList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE_LIST.writeId(this, buffer);
		
		final int attemptsRemainingCountBgrade = _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_B_GRADE) > 0 ? _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_B_GRADE) : 0;
		final int attemptsRemainingCountAgrade = _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_A_GRADE) > 0 ? _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_A_GRADE) : 0;
		final int maxRelicsOnConfirmationList = Config.RELIC_UNCONFIRMED_LIST_LIMIT;
		final long defaultReplacementTime = Config.RELIC_UNCONFIRMED_TIME_LIMIT * 86400000; // 86400000 = 1 day in milliseconds
		
		long relicSummonTime = 0;
		final List<Integer> unconfirmedRelics = new ArrayList<>();
		for (PlayerRelicData relic : _player.getRelics())
		{
			if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() == 1)) // Unconfirmed relics are set on summon to index 300 and up.
			{
				unconfirmedRelics.add(relic.getRelicId());
				relicSummonTime = relic.getRelicSummonTime();
			}
		}
		
		buffer.writeInt(maxRelicsOnConfirmationList);
		buffer.writeInt(unconfirmedRelics.size()); // Confirmation relics array size.
		for (int i = 0; i < unconfirmedRelics.size(); i++)
		{
			final int unconfirmedRelicGrade = RelicData.getInstance().getRelic(unconfirmedRelics.get(i)).getGrade();
			buffer.writeInt(i); // List position.
			buffer.writeInt(unconfirmedRelics.get(i)); // Relic Id.
			buffer.writeInt(unconfirmedRelicGrade == 4 ? attemptsRemainingCountBgrade : attemptsRemainingCountAgrade); // Exchange attempts remaining based on relic grade.
			buffer.writeInt(5); // Exchange attempts max.
			buffer.writeInt((int) ((relicSummonTime + defaultReplacementTime) / 1000)); // Replacement Time.
		}
	}
}
