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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

/**
 * @author Mobius, Norvox
 */
public class LimitShopCraftData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(LimitShopData.class.getName());
	
	private final List<LimitShopProductHolder> _products = new ArrayList<>();
	
	protected LimitShopCraftData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_products.clear();
		parseDatapackFile("data/LimitShopCraft.xml");
		
		if (!_products.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _products.size() + " items.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				final NamedNodeMap at = n.getAttributes();
				final Node attribute = at.getNamedItem("enabled");
				if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("product".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							final StatSet set = new StatSet();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								att = attrs.item(i);
								set.set(att.getNodeName(), att.getNodeValue());
							}
							
							final int id = parseInteger(attrs, "id");
							final int category = parseInteger(attrs, "category");
							final int minLevel = parseInteger(attrs, "minLevel", 1);
							final int maxLevel = parseInteger(attrs, "maxLevel", 999);
							final int[] ingredientIds = new int[5];
							ingredientIds[0] = 0;
							ingredientIds[1] = 0;
							ingredientIds[2] = 0;
							ingredientIds[3] = 0;
							ingredientIds[4] = 0;
							final long[] ingredientQuantities = new long[5];
							ingredientQuantities[0] = 0;
							ingredientQuantities[1] = 0;
							ingredientQuantities[2] = 0;
							ingredientQuantities[3] = 0;
							ingredientQuantities[4] = 0;
							final int[] ingredientEnchants = new int[5];
							ingredientEnchants[0] = 0;
							ingredientEnchants[1] = 0;
							ingredientEnchants[2] = 0;
							ingredientEnchants[3] = 0;
							ingredientEnchants[4] = 0;
							int productionId = 0;
							int productionId2 = 0;
							int productionId3 = 0;
							int productionId4 = 0;
							int productionId5 = 0;
							long count = 1L;
							long count2 = 1L;
							long count3 = 1L;
							long count4 = 1L;
							long count5 = 1L;
							float chance = 100f;
							float chance2 = 100f;
							float chance3 = 100f;
							float chance4 = 100f;
							boolean announce = false;
							boolean announce2 = false;
							boolean announce3 = false;
							boolean announce4 = false;
							boolean announce5 = false;
							int enchant = 0;
							int accountDailyLimit = 0;
							int accountWeeklyLimit = 0;
							int accountMonthlyLimit = 0;
							int accountBuyLimit = 0;
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								attrs = b.getAttributes();
								
								if ("ingredient".equalsIgnoreCase(b.getNodeName()))
								{
									final int ingredientId = parseInteger(attrs, "id");
									final long ingredientQuantity = parseLong(attrs, "count", 1L);
									final int ingredientEnchant = parseInteger(attrs, "enchant", 0);
									
									if (ingredientId > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(ingredientId);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId + " productId: " + id);
											continue;
										}
										
										if ((ingredientQuantity > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + ingredientId + " should be stackable!");
										}
									}
									
									if (ingredientIds[0] == 0)
									{
										ingredientIds[0] = ingredientId;
									}
									else if (ingredientIds[1] == 0)
									{
										ingredientIds[1] = ingredientId;
									}
									else if (ingredientIds[2] == 0)
									{
										ingredientIds[2] = ingredientId;
									}
									else if (ingredientIds[3] == 0)
									{
										ingredientIds[3] = ingredientId;
									}
									else
									{
										ingredientIds[4] = ingredientId;
									}
									
									if (ingredientQuantities[0] == 0)
									{
										ingredientQuantities[0] = ingredientQuantity;
									}
									else if (ingredientQuantities[1] == 0)
									{
										ingredientQuantities[1] = ingredientQuantity;
									}
									else if (ingredientQuantities[2] == 0)
									{
										ingredientQuantities[2] = ingredientQuantity;
									}
									else if (ingredientQuantities[3] == 0)
									{
										ingredientQuantities[3] = ingredientQuantity;
									}
									else
									{
										ingredientQuantities[4] = ingredientQuantity;
									}
									
									if (ingredientEnchants[0] == 0)
									{
										ingredientEnchants[0] = ingredientEnchant;
									}
									else if (ingredientEnchants[1] == 0)
									{
										ingredientEnchants[1] = ingredientEnchant;
									}
									else if (ingredientEnchants[2] == 0)
									{
										ingredientEnchants[2] = ingredientEnchant;
									}
									else if (ingredientEnchants[3] == 0)
									{
										ingredientEnchants[3] = ingredientEnchant;
									}
									else
									{
										ingredientEnchants[4] = ingredientEnchant;
									}
								}
								else if ("production".equalsIgnoreCase(b.getNodeName()))
								{
									productionId = parseInteger(attrs, "id");
									count = parseLong(attrs, "count", 1L);
									chance = parseFloat(attrs, "chance", 100f);
									announce = parseBoolean(attrs, "announce", false);
									enchant = parseInteger(attrs, "enchant", 0);
									productionId2 = parseInteger(attrs, "id2", 0);
									count2 = parseLong(attrs, "count2", 1L);
									chance2 = parseFloat(attrs, "chance2", 100f);
									announce2 = parseBoolean(attrs, "announce2", false);
									productionId3 = parseInteger(attrs, "id3", 0);
									count3 = parseLong(attrs, "count3", 1L);
									chance3 = parseFloat(attrs, "chance3", 100f);
									announce3 = parseBoolean(attrs, "announce3", false);
									productionId4 = parseInteger(attrs, "id4", 0);
									count4 = parseLong(attrs, "count4", 1L);
									chance4 = parseFloat(attrs, "chance4", 100f);
									announce4 = parseBoolean(attrs, "announce4", false);
									productionId5 = parseInteger(attrs, "id5", 0);
									count5 = parseLong(attrs, "count5", 1L);
									announce5 = parseBoolean(attrs, "announce5", false);
									accountDailyLimit = parseInteger(attrs, "accountDailyLimit", 0);
									accountWeeklyLimit = parseInteger(attrs, "accountWeeklyLimit", 0);
									accountMonthlyLimit = parseInteger(attrs, "accountMonthlyLimit", 0);
									accountBuyLimit = parseInteger(attrs, "accountBuyLimit", 0);
									
									if (productionId > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(productionId);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId + " productId: " + id);
											continue;
										}
										
										if ((count > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + productionId + " should be stackable!");
										}
									}
									if (productionId2 > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(productionId2);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId2 + " productId: " + id);
											continue;
										}
										
										if ((count2 > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + productionId2 + " should be stackable!");
										}
									}
									if (productionId3 > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(productionId3);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId3 + " productId: " + id);
											continue;
										}
										
										if ((count3 > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + productionId3 + " should be stackable!");
										}
									}
									if (productionId4 > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(productionId4);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId4 + " productId: " + id);
											continue;
										}
										
										if ((count4 > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + productionId4 + " should be stackable!");
										}
									}
									if (productionId5 > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(productionId5);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId5 + " productId: " + id);
											continue;
										}
										
										if ((count5 > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + productionId5 + " should be stackable!");
										}
									}
									
									if (productionId2 == 0)
									{
										chance = 100;
									}
									if (productionId3 == 0)
									{
										chance2 = 100;
									}
									if (productionId4 == 0)
									{
										chance3 = 100;
									}
									if (productionId5 == 0)
									{
										chance4 = 100;
									}
								}
							}
							
							_products.add(new LimitShopProductHolder(id, category, minLevel, maxLevel, ingredientIds, ingredientQuantities, ingredientEnchants, productionId, count, chance, announce, enchant, productionId2, count2, chance2, announce2, productionId3, count3, chance3, announce3, productionId4, count4, chance4, announce4, productionId5, count5, announce5, accountDailyLimit, accountWeeklyLimit, accountMonthlyLimit, accountBuyLimit));
						}
					}
				}
			}
		}
	}
	
	public LimitShopProductHolder getProduct(int id)
	{
		for (LimitShopProductHolder product : _products)
		{
			if (product.getId() == id)
			{
				return product;
			}
		}
		return null;
	}
	
	public List<LimitShopProductHolder> getProducts()
	{
		return _products;
	}
	
	public static LimitShopCraftData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LimitShopCraftData INSTANCE = new LimitShopCraftData();
	}
}