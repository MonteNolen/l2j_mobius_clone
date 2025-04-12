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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsUpgrade extends ServerPacket
{
	private final Player _player;
	private final boolean _success;
	private final int _relicId;
	private final int _relicLevel;
	
	public ExRelicsUpgrade(Player player, boolean success, int relicId, int relicLevel)
	{
		_player = player;
		_success = success;
		_relicId = relicId;
		_relicLevel = relicLevel;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_UPGRADE.writeId(this, buffer);
		
		// Take item fee based on relic grade.
		int itemFeeId = 0;
		long itemFeeCount = 0;
		switch (RelicData.getInstance().getRelic(_relicId).getGrade())
		{
			case 1:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_NO_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_NO_GRADE.get(0).getCount();
				break;
			}
			case 2:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_D_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_D_GRADE.get(0).getCount();
				break;
			}
			case 3:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_C_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_C_GRADE.get(0).getCount();
				break;
			}
			case 4:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_B_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_B_GRADE.get(0).getCount();
				break;
			}
			case 5:
			case 6:
			case 7:
			{
				itemFeeId = Config.RELIC_ENHANCEMENT_FEE_A_GRADE.get(0).getId();
				itemFeeCount = Config.RELIC_ENHANCEMENT_FEE_A_GRADE.get(0).getCount();
				break;
			}
		}
		
		_player.destroyItemByItemId(ItemProcessType.FEE, itemFeeId, itemFeeCount, _player, true);
		
		buffer.writeByte(_success);
		buffer.writeInt(_relicId);
		buffer.writeInt(_relicLevel);
	}
}
