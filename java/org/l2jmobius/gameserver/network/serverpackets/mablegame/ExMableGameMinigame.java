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
package org.l2jmobius.gameserver.network.serverpackets.mablegame;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ExMableGameMinigame extends ServerPacket
{
	private final int _bossType;
	private final int _luckyNumber;
	private final int _dice;
	private final int _bossDice;
	private final int _result;
	private final boolean _isLuckyNumber;
	private final int _rewardItemId;
	private final long _rewardItemCount;
	
	public ExMableGameMinigame(int bossType, int luckyNumber, int dice, int bossDice, int result, boolean isLuckyNumber, int rewardItemId, long rewardItemCount)
	{
		_bossType = bossType;
		_luckyNumber = luckyNumber;
		_dice = dice;
		_bossDice = bossDice;
		_result = result;
		_isLuckyNumber = isLuckyNumber;
		_rewardItemId = rewardItemId;
		_rewardItemCount = rewardItemCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MABLE_GAME_MINIGAME.writeId(this, buffer);
		buffer.writeInt(_bossType);
		buffer.writeInt(_luckyNumber);
		buffer.writeInt(_dice);
		buffer.writeInt(_bossDice);
		buffer.writeByte(_result);
		buffer.writeByte(_isLuckyNumber);
		buffer.writeInt(_rewardItemId);
		buffer.writeLong(_rewardItemCount);
	}
}
