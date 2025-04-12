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

import org.l2jmobius.Config;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsCombination extends ServerPacket
{
	private final Player _player;
	private final int _relicsUsedGrade;
	private final int _relicsUsedCount;
	
	public ExRelicsCombination(Player player, int relicsUsedGrade, int relicsUsedCount)
	{
		_player = player;
		_relicsUsedGrade = relicsUsedGrade;
		_relicsUsedCount = relicsUsedCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_COMBINATION.writeId(this, buffer);
		int chance = 0;
		switch (_relicsUsedGrade)
		{
			case 1: // Common Dolls Compound.
			{
				chance = Config.RELIC_COMPOUND_COMMON_DOLLS_CHANCE_ENHANCED;
				break;
			}
			case 2: // Enhanced Dolls Compound.
			{
				chance = Config.RELIC_COMPOUND_ENHANCED_DOLLS_CHANCE_SUPERIOR;
				break;
			}
			case 3: // Superior Dolls Compound.
			{
				chance = Config.RELIC_COMPOUND_SUPERIOR_DOLLS_CHANCE_RARE;
				break;
			}
			case 4: // Rare Dolls Compound.
			{
				chance = Config.RELIC_COMPOUND_RARE_DOLLS_CHANCE_HEROIC;
				break;
			}
			case 5: // Heroic Dolls Compound.
			{
				chance = Config.RELIC_COMPOUND_HEROIC_DOLLS_CHANCE_LEGENDARY;
				break;
			}
			case 6: // Legendary Dolls Compound.
			{
				chance = Config.RELIC_COMPOUND_LEGENDARY_DOLLS_CHANCE_MYTHIC;
				break;
			}
		}
		
		final int arraySize = _relicsUsedCount / 4;
		buffer.writeByte(true); // If not true the compound result page is not showing up.
		buffer.writeInt(arraySize); // Obtained relics count array size.
		
		int feeItemId = 0;
		long feeItemCount = 0;
		int itemsOnFailureCount = 0;
		int shardId = 0;
		long shardCount = 0;
		int obtainedRelicId = 0;
		int relicsFailedCount = 0;
		int relicsSuccededCount = 0;
		// Loop each obtained relic.
		for (int i = 0; i < arraySize; i++)
		{
			// Set fees and obtained relic based on ingredients used grade.
			switch (_relicsUsedGrade)
			{
				case 1: // Common Dolls Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_NO_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_NO_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size())); // Random Enhanced doll id.
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size())); // Random Common doll id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 2: // Enhanced Dolls Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_D_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_D_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size())); // Random Superior doll id.
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size())); // Random Enhanced doll id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 3: // Superior Dolls Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_D_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_D_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size())); // Random Rare doll id.
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size())); // Random Superior doll id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 4: // Rare Dolls Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_C_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_C_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size())); // Random Heroic doll id.
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size())); // Random Rare doll id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 5: // Heroic Dolls Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_C_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_C_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size())); // Random Legendary doll id.
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size())); // Random Heroic doll id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
				case 6: // Legendary Dolls Compound.
				{
					feeItemId = Config.RELIC_COMPOUND_FEE_B_GRADE.get(0).getId();
					feeItemCount = Config.RELIC_COMPOUND_FEE_B_GRADE.get(0).getCount();
					if (Rnd.get(100) < chance)
					{
						obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size())); // Random Mythic doll id.
						relicsSuccededCount++;
					}
					else
					{
						obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size())); // Random Legendary doll id.
						relicsFailedCount++;
					}
					buffer.writeInt(obtainedRelicId);
					break;
				}
			}
			
			// Add to DB Table the obtained relics.
			final Collection<PlayerRelicData> storedRelics = _player.getRelics();
			
			// Check if the relic with the same ID exists.
			PlayerRelicData existingRelic = null;
			for (PlayerRelicData relic : storedRelics)
			{
				if (relic.getRelicId() == obtainedRelicId)
				{
					existingRelic = relic;
					break;
				}
			}
			
			final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
			// Increment the count of the existing relic.
			if (existingRelic != null)
			{
				// Increment the count of the existing relic.
				existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
				_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, existingRelic.getRelicCount() + 1)); // Update confirmed relic list with new relic.
				if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					_player.sendMessage("Existing relic id: " + obtainedRelicId + " count increased.");
				}
				if (existingRelic.getRelicIndex() == 0)
				{
					if (!_player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
					{
						// Auto-Add to relic collections on summon.
						_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
					}
				}
			}
			// Add the new relic if it doesn't exist.
			else
			{
				newRelic.setRelicIndex(0);
				storedRelics.add(newRelic);
				_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
				if (newRelic.getRelicIndex() == 0)
				{
					if (!_player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
					{
						// Auto-Add to relic collections on summon.
						_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
					}
				}
			}
		}
		// Show obtained items when failed.
		buffer.writeInt(itemsOnFailureCount); // Obtained items when failed array size.
		buffer.writeInt(shardId); // Item 1 id.
		buffer.writeLong(shardCount); // Item 1 count.
		
		_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
		_player.sendMessage("You obtained through compounding: " + arraySize + " relics.");
		_player.destroyItemByItemId(ItemProcessType.FEE, feeItemId, feeItemCount * arraySize, _player, true);
		// Send summary of compounds
		_player.sendMessage("Relics compound summary: " + relicsSuccededCount + " succeded and " + relicsFailedCount + " failed.");
		if (relicsFailedCount > relicsSuccededCount)
		{
			_player.sendPacket(new ExShowScreenMessage("Relics compound has failed.", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
			if (_relicsUsedGrade >= 3)
			{
				// Add failure items obtained here
				_player.addItem(ItemProcessType.FEE, shardId, shardCount * relicsFailedCount, _player, true);
			}
		}
	}
}
