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

import java.util.Collection;
import java.util.List;

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsExchange extends ServerPacket
{
	private final Player _player;
	private final int _index;
	private int _relicId;
	private boolean _success;
	
	public ExRelicsExchange(Player player, int index, int relicId)
	{
		_player = player;
		_index = index;
		_relicId = relicId;
		_success = false;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE.writeId(this, buffer);
		
		int attemptsRemaining = 0;
		int maxAttempts = 0;
		int obtainedRelicId = 0;
		final int unconfirmedRelicGrade = RelicData.getInstance().getRelic(_relicId).getGrade();
		switch (unconfirmedRelicGrade)
		{
			case 4:
			{
				maxAttempts = Config.RELIC_REPLACE_ATTEMPTS_B_GRADE;
				attemptsRemaining = _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, maxAttempts); // Exchange attempts remaining
				if (Rnd.get(100) < (Config.RELIC_REPLACE_ATTEMPTS_CHANCE_SHINING_B_GRADE))
				{
					obtainedRelicId = getRandomRelicId(_relicId, Config.B_GRADE_SHINING_RELICS);
					_success = true;
					break;
				}
				obtainedRelicId = getRandomRelicId(_relicId, Config.B_GRADE_COMMON_RELICS);
				_success = true;
				break;
			}
			case 5:
			case 6:
			case 7:
			{
				maxAttempts = Config.RELIC_REPLACE_ATTEMPTS_A_GRADE;
				attemptsRemaining = _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, maxAttempts); // Exchange attempts remaining
				obtainedRelicId = getRandomRelicId(_relicId, Config.A_GRADE_COMMON_RELICS);
				_success = true;
				break;
			}
		}
		
		final Collection<PlayerRelicData> storedRelics = _player.getRelics();
		// Check if the relic with the same ID exists.
		PlayerRelicData unconfirmedRelic = null;
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicId() == _relicId) && (relic.getRelicIndex() >= 300) && (relic.getRelicCount() != 0)) // Only relics with index >= 300 can be exchanged.
			{
				unconfirmedRelic = relic;
				break;
			}
		}
		
		final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 1, 300, System.currentTimeMillis());
		if (unconfirmedRelic != null)
		{
			// Decrease the count of the existing relic
			unconfirmedRelic.setRelicCount(unconfirmedRelic.getRelicCount() - 1);
			_relicId = newRelic.getRelicId();
			storedRelics.add(newRelic);
			
			if (unconfirmedRelicGrade == 4)
			{
				_player.getAccountVariables().set(AccountVariables.B_GRADE_RELIC_ATEMPTS, _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_B_GRADE) - 1);
				attemptsRemaining = attemptsRemaining - 1;
			}
			else
			{
				_player.getAccountVariables().set(AccountVariables.A_GRADE_RELIC_ATEMPTS, _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_A_GRADE) - 1);
				attemptsRemaining = attemptsRemaining - 1;
			}
			_player.getAccountVariables().storeMe();
			_player.storeRelics();
			int currentBGradeAttempts = _player.getAccountVariables().getInt(AccountVariables.B_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_B_GRADE);
			int currentAGradeAttempts = _player.getAccountVariables().getInt(AccountVariables.A_GRADE_RELIC_ATEMPTS, Config.RELIC_REPLACE_ATTEMPTS_A_GRADE);
			int replacementItemId = 0;
			Long replacementFee = 0L;
			switch (unconfirmedRelicGrade)
			{
				case 4: // B Grade Relics
				{
					for (int i = 0; i <= Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.size(); i++)
					{
						if ((currentBGradeAttempts + 1) == (Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.size() - i))
						{
							if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
							{
								_player.sendMessage("Fees list size:" + Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.size());
								_player.sendMessage("currentBGradeAttempts value: " + currentBGradeAttempts);
								_player.sendMessage("index value: " + i);
							}
							replacementItemId = Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.isEmpty() ? 0 : Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.get(i).getId();
							replacementFee = Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.isEmpty() ? 0 : Config.RELIC_REPLACE_ATTEMPTS_FEES_B_GRADE.get(i).getCount();
						}
					}
					break;
				}
				case 5: // A Grade Relics
				case 6:
				case 7:
				{
					for (int i = 0; i <= Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.size(); i++)
					{
						if ((currentAGradeAttempts + 1) == (Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.size() - i))
						{
							if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
							{
								_player.sendMessage("Fees list size:" + Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.size());
								_player.sendMessage("currentBGradeAttempts value: " + currentAGradeAttempts);
								_player.sendMessage("index value: " + i);
							}
							replacementItemId = Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.isEmpty() ? 0 : Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.get(i).getId();
							replacementFee = Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.isEmpty() ? 0 : Config.RELIC_REPLACE_ATTEMPTS_FEES_A_GRADE.get(i).getCount();
						}
					}
					break;
				}
			}
			_player.destroyItemByItemId(ItemProcessType.FEE, replacementItemId, replacementFee, _player, true);
			_player.getAccountVariables().storeMe();
		}
		
		buffer.writeInt(_index);
		buffer.writeByte(_success);
		buffer.writeInt(attemptsRemaining); // Exchange attempts remaining.
		buffer.writeInt(maxAttempts); // Exchange attempts max.
		buffer.writeInt(_relicId);
		_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
	}
	
	private int getRandomRelicId(int currentRelicId, List<Integer> relicIds)
	{
		int result = currentRelicId;
		while (result == currentRelicId)
		{
			result = relicIds.get(Rnd.get(relicIds.size()));
		}
		return result;
	}
}
