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
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author CostyKiller, Mobius, fruit
 */
public class ExRelicsSummonResult extends ServerPacket
{
	private final Player _player;
	private final int _relicCouponItemId;
	private final int _relicSummonCount;
	
	public ExRelicsSummonResult(Player player, int relicCouponItemId, int relicSummonCount)
	{
		_player = player;
		_relicCouponItemId = relicCouponItemId;
		_relicSummonCount = player.getInventory().getItemByItemId(_relicCouponItemId) == null ? 0 : relicSummonCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_SUMMON_RESULT.writeId(this, buffer);
		
		buffer.writeByte(true); // Only works with true.
		buffer.writeInt(_relicCouponItemId); // Summon item id.
		buffer.writeInt(_relicSummonCount); // Array size of obtained relics.
		
		final RelicCouponHolder relicCouponHolder = RelicCouponData.getInstance().getRelicIdFromCouponId(_relicCouponItemId);
		if (relicCouponHolder != null)
		{
			final int obtainedRelicId = relicCouponHolder.getRelicId();
			final RelicDataHolder obtainedRelicTemplate = RelicData.getInstance().getRelic(obtainedRelicId);
			if (obtainedRelicTemplate != null)
			{
				buffer.writeInt(obtainedRelicId);
				
				// Add to database table the obtained relics.
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
				if (existingRelic != null)
				{
					existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
					_player.storeRelics();
					_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, 1)); // Update confirmed relic list with new relic.
					// Announce the existing obtained relic.
					if (Config.RELIC_SUMMON_ANNOUNCE && !Config.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
					{
						Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
					}
					// Check if relic is already registered in some collection.
					if (!_player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
					{
						// Auto-Add to relic collections on summon.
						_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
					}
				}
				else
				{
					storedRelics.add(newRelic);
					_player.storeRelics();
					_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("2.New relic id: " + newRelic.getRelicId() + " was added to relic list.");
					}
					if (Config.RELIC_SUMMON_ANNOUNCE && !Config.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
					{
						// Announce the new obtained relic
						Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
					}
					if (!_player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
					{
						// Auto-Add to relic collections on summon.
						_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
					}
				}
				_player.storeRelics();
				_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
				_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
				_player.giveRelicSkill(obtainedRelicTemplate);
				return;
			}
		}
		
		// Obtained relics by scroll type.
		int obtainedRelicId = 0;
		for (int i = 1; i <= _relicSummonCount; i++)
		{
			if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				_player.sendMessage("I = " + i);
			}
			
			// Doll Summon Coupon (Common - Enhanced - Superior).
			if (Config.COMMON_TO_SUPERIOR_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				if (relicChance < Config.COMMON_TO_SUPERIOR_DOLL_SUPERIOR_CHANCE)
				{
					obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
				}
				else if (relicChance < Config.COMMON_TO_SUPERIOR_DOLL_ENHANCED_CHANCE)
				{
					obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
				}
				else
				{
					obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size()));
				}
			}
			// Doll Summon Coupon (Common - Enhanced - Superior - Rare).
			if (Config.COMMON_TO_RARE_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				if (relicChance < Config.COMMON_TO_RARE_DOLL_RARE_CHANCE)
				{
					obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
				}
				else if (relicChance < Config.COMMON_TO_RARE_DOLL_SUPERIOR_CHANCE)
				{
					obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
				}
				else if (relicChance < Config.COMMON_TO_RARE_DOLL_ENHANCED_CHANCE)
				{
					obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
				}
				else
				{
					obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size()));
				}
			}
			// Doll Summon Coupon (Enhanced - Superior).
			if (Config.ENHANCED_TO_SUPERIOR_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				final int relicChance = Rnd.get(100);
				if (relicChance < Config.ENHANCED_TO_SUPERIOR_DOLL_SUPERIOR_CHANCE)
				{
					obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
				}
				else
				{
					obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
				}
			}
			// Doll Summon Coupon (Common).
			else if (Config.COMMON_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.NO_GRADE_COMMON_RELICS.get(Rnd.get(Config.NO_GRADE_COMMON_RELICS.size()));
			}
			// Doll Summon Coupon (Enhanced).
			else if (Config.ENHANCED_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.D_GRADE_COMMON_RELICS.get(Rnd.get(Config.D_GRADE_COMMON_RELICS.size()));
			}
			// Doll Summon Coupon (Superior).
			else if (Config.SUPERIOR_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.D_GRADE_SHINING_RELICS.get(Rnd.get(Config.D_GRADE_SHINING_RELICS.size()));
			}
			// Doll Summon Coupon (Rare).
			else if (Config.RARE_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.C_GRADE_COMMON_RELICS.get(Rnd.get(Config.C_GRADE_COMMON_RELICS.size()));
			}
			// Doll Summon Coupon (Heroic).
			else if (Config.HEROIC_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.C_GRADE_SHINING_RELICS.get(Rnd.get(Config.C_GRADE_SHINING_RELICS.size()));
			}
			// Doll Summon Coupon (Legendary).
			else if (Config.LEGENDARY_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.B_GRADE_COMMON_RELICS.get(Rnd.get(Config.B_GRADE_COMMON_RELICS.size()));
			}
			// Doll Summon Coupon (Mythic).
			else if (Config.MYTHIC_DOLL_SUMMON_COUPONS.contains(_relicCouponItemId))
			{
				obtainedRelicId = Config.B_GRADE_SHINING_RELICS.get(Rnd.get(Config.B_GRADE_SHINING_RELICS.size()));
			}
			
			buffer.writeInt(obtainedRelicId);
			
			// Add to database table the obtained relics.
			Collection<PlayerRelicData> storedRelics = _player.getRelics();
			
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
			
			final RelicDataHolder obtainedRelicTemplate = RelicData.getInstance().getRelic(obtainedRelicId);
			if (obtainedRelicTemplate != null)
			{
				final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
				if (existingRelic != null)
				{
					existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
					_player.storeRelics();
					_player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, 1)); // Update confirmed relic list with new relic.
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("2.Existing relic id: " + existingRelic.getRelicId() + " count was updated.");
					}
					// Announce the existing obtained relic.
					if (Config.RELIC_SUMMON_ANNOUNCE && !Config.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
					{
						Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, existingRelic.getRelicId()));
					}
					// Check if relic is already registered in some collection.
					if (!_player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
					{
						// Auto-Add to relic collections on summon.
						_player.sendPacket(new ExRelicsCollectionUpdate(_player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
					}
				}
				else
				{
					storedRelics.add(newRelic);
					_player.storeRelics();
					_player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
					if (Config.RELIC_SYSTEM_DEBUG_ENABLED)
					{
						_player.sendMessage("2.New relic id: " + newRelic.getRelicId() + " was added to relic list.");
					}
					if (Config.RELIC_SUMMON_ANNOUNCE && !Config.RELIC_ANNOUNCE_ONLY_A_B_GRADE)
					{
						// Announce the new obtained relic
						Broadcast.toAllOnlinePlayers(new ExRelicsAnnounce(_player, newRelic.getRelicId()));
					}
					if (!_player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
					{
						// Auto-Add to relic collections on summon.
						_player.sendPacket(new ExRelicsCollectionUpdate(_player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
					}
				}
				_player.storeRelics();
				_player.giveRelicSkill(obtainedRelicTemplate);
			}
			else
			{
				PacketLogger.warning("ExRelicsSummonResult: Relic coupon " + _relicCouponItemId + " is probably not registred in configs.");
			}
			_player.sendPacket(new ExRelicsList(_player)); // Update confirmed relic list relics count.
			_player.sendPacket(new ExRelicsExchangeList(_player)); // Update relic exchange/confirm list.
		}
	}
}
