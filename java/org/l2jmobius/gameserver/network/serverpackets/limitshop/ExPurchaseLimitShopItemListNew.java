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
package org.l2jmobius.gameserver.network.serverpackets.limitshop;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPurchaseLimitShopItemListNew extends ServerPacket
{
	private final Player _player;
	private final int _shopType; // 3 Lcoin Store, 4 Special Craft, 100 Clan Shop
	private final int _page;
	private final int _totalPages;
	private final Collection<LimitShopProductHolder> _products;
	
	public ExPurchaseLimitShopItemListNew(Player player, int shopType, int page, int totalPages, Collection<LimitShopProductHolder> products)
	{
		_player = player;
		_shopType = shopType;
		_page = page;
		_totalPages = totalPages;
		_products = products;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PURCHASE_LIMIT_SHOP_ITEM_LIST_NEW.writeId(this, buffer);
		buffer.writeByte(_shopType);
		buffer.writeByte(_page); // 311
		buffer.writeByte(_totalPages); // 311
		buffer.writeInt(_products.size());
		for (LimitShopProductHolder product : _products)
		{
			buffer.writeInt(product.getId());
			buffer.writeInt(product.getProductionId());
			buffer.writeInt(product.getIngredientIds()[0]);
			buffer.writeInt(product.getIngredientIds()[1]);
			buffer.writeInt(product.getIngredientIds()[2]);
			buffer.writeInt(product.getIngredientIds()[3]); // 306
			buffer.writeInt(product.getIngredientIds()[4]); // 306
			buffer.writeLong(product.getIngredientQuantities()[0]);
			buffer.writeLong(product.getIngredientQuantities()[1]);
			buffer.writeLong(product.getIngredientQuantities()[2]);
			buffer.writeLong(product.getIngredientQuantities()[3]); // 306
			buffer.writeLong(product.getIngredientQuantities()[4]); // 306
			buffer.writeShort(product.getIngredientEnchants()[0]);
			buffer.writeShort(product.getIngredientEnchants()[1]);
			buffer.writeShort(product.getIngredientEnchants()[2]);
			buffer.writeShort(product.getIngredientEnchants()[3]); // 306
			buffer.writeShort(product.getIngredientEnchants()[4]); // 306
			// Check limits.
			if (product.getAccountDailyLimit() > 0) // Sale period.
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + product.getProductionId(), 0) >= product.getAccountDailyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountDailyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + product.getProductionId(), 0));
				}
			}
			else if (product.getAccountWeeklyLimit() > 0)
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + product.getProductionId(), 0) >= product.getAccountWeeklyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountWeeklyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + product.getProductionId(), 0));
				}
			}
			else if (product.getAccountMonthlyLimit() > 0)
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + product.getProductionId(), 0) >= product.getAccountMonthlyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountMonthlyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + product.getProductionId(), 0));
				}
			}
			else if (product.getAccountBuyLimit() > 0) // Count limit.
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + product.getProductionId(), 0) >= product.getAccountBuyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountBuyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + product.getProductionId(), 0));
				}
			}
			else // No account limits.
			{
				buffer.writeInt(1);
			}
			buffer.writeInt(0); // nRemainSec
			buffer.writeInt(0); // nRemainServerItemAmount
			buffer.writeShort(0); // sCircleNum (311)
			buffer.writeInt(0); // nStartTime (464)
		}
	}
}
