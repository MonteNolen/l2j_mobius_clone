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
package ai.others.AdditionalServicesAdvisor;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.player.SubclassInfoType;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.ExSubjobInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoInvenWeight;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class AdditionalServicesAdvisor extends AbstractNpcAI
{
	// NPCs
	private static final int JUNI = 34152;
	private static final int HERMIN = 34153;
	// Items
	private static final int CLASS_CHANGE_COUPON = 94828;
	private static final int SP_SCROLL = 94829;
	private static final int SPELLBOOK_HUMAN = 90038; // Spellbook: Mount Golden Lion
	private static final int SPELLBOOK_ELF = 90039; // Spellbook: Mount Pegasus
	private static final int SPELLBOOK_DELF = 90040; // Spellbook: Mount Saber Tooth Cougar
	private static final int SPELLBOOK_ORC = 90042; // Spellbook: Mount Black Bear
	private static final int SPELLBOOK_DWARF = 90041; // Spellbook: Mount Kukuru
	private static final int SPELLBOOK_KAMAEL = 91946; // Spellbook: Mount Griffin
	private static final int SPELLBOOK_DEATH_KNIGHT = 93383; // Spellbook: Mount Nightmare Steed
	private static final int SPELLBOOK_SYLPH = 95367; // Spellbook: Mount Elemental Lyn Draco
	// Misc
	private static final Location TELEPORT_ADEN_LOCATION = new Location(146856, 25803, -2008);
	private static final Location TELEPORT_GIRAN_LOCATION = new Location(83386, 148014, -3400);
	private static final int INSTANCE_ID = 190;
	
	private AdditionalServicesAdvisor()
	{
		addStartNpc(JUNI, HERMIN);
		addFirstTalkId(JUNI, HERMIN);
		addTalkId(JUNI, HERMIN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String html = null;
		switch (npc.getId())
		{
			case JUNI:
			{
				switch (event)
				{
					case "34152.htm":
					case "34152-1.htm":
					{
						html = event;
						break;
					}
					case "teleport_inside":
					{
						html = checkConditions(npc, player);
						if (html == null)
						{
							final Instance instance = InstanceManager.getInstance().createInstance(INSTANCE_ID, player);
							player.teleToLocation(instance.getEnterLocation(), instance);
						}
						break;
					}
				}
				break;
			}
			case HERMIN:
			{
				switch (event)
				{
					case "34153.htm":
					case "34153-1.htm":
					case "34153-class_darkelf.htm":
					case "34153-class_dwarf.htm":
					case "34153-class_elf.htm":
					case "34153-class_human.htm":
					case "34153-class_kamael.htm":
					case "34153-class_orc.htm":
					case "34153-class_sylph.htm":
					case "34153-race.htm":
					case "34153-class_highelf.htm":
					{
						html = event;
						break;
					}
					case "teleport_aden":
					{
						player.teleToLocation(TELEPORT_ADEN_LOCATION, null);
						break;
					}
					case "teleport_giran":
					{
						player.teleToLocation(TELEPORT_GIRAN_LOCATION, null);
						break;
					}
				}
				if (event.startsWith("choose_class"))
				{
					final int classId = Integer.parseInt(event.split(" ")[1]);
					if (classId > 195)
					{
						html = getHtm(player, "34153-choose_gender_unavailable.htm");
					}
					else
					{
						html = getHtm(player, "34153-choose_gender.htm");
					}
					
					html = html.replace("%class_id%", String.valueOf(classId));
					html = html.replace("%class_name%", ClassListData.getInstance().getClass(classId).getClassName());
					return html;
				}
				else if (event.startsWith("choose_gender"))
				{
					final String[] split = event.split(" ");
					final int classId = Integer.parseInt(split[1]);
					boolean female = split[2].equals("female");
					if (female && (((classId > 195) && (classId < 225)) || ((classId > 239) && (classId < 244)))) // Death Knight / Human Assassins / High Elf Tank
					{
						player.sendMessage("You can't choose female for these classes. Changed to male.");
						female = false;
					}
					
					if (!female && (classId > 224) && (classId < 240))
					{
						player.sendMessage("You can't choose male for these classes. Changed to female.");
						female = true;
					}
					
					html = getHtm(player, "34153-choose_gender_confirm.htm");
					html = html.replace("%class_id%", String.valueOf(classId));
					html = html.replace("%class_name%", ClassListData.getInstance().getClass(classId).getClassName());
					html = html.replace("%sex%", female ? "female" : "male");
				}
				else if (event.startsWith("choose_gender_confirm"))
				{
					final String[] split = event.split(" ");
					final int classId = Integer.parseInt(split[1]);
					final boolean female = split[2].equals("female");
					if (female && (((classId > 195) && (classId < 225)) || ((classId > 239) && (classId < 244)))) // Death Knight / Human Assassins / High Elf Tank
					{
						player.sendMessage("You can't choose female for these classes.");
						return null;
					}
					
					if (!female && (classId > 224) && (classId < 240))
					{
						player.sendMessage("You can't choose male for these classes.");
						return null;
					}
					
					html = getHtm(player, "34153-choose_gender_unavailable.htm");
					html = html.replace("%class_id%", String.valueOf(classId));
					html = html.replace("%class_name%", ClassListData.getInstance().getClass(classId).getClassName());
					html = html.replace("%sex%", female ? "female" : "male");
				}
				else if (event.startsWith("confirm_class"))
				{
					final PlayerAppearance appearance = player.getAppearance();
					
					html = checkConditions(npc, player);
					if (html != null)
					{
						break;
					}
					
					final String[] split = event.split(" ");
					final int classId = Integer.parseInt(split[1]);
					
					if (player.getPlayerClass().getId() == classId)
					{
						player.sendMessage("You're in the same class now.");
						break;
					}
					
					final boolean female = split[2].equals("female");
					
					if (((classId > 195) && (classId < 225)) || ((classId > 239) && (classId < 244))) // Death Knight / Human Assassins / High Elf Tank
					{
						appearance.setMale();
					}
					
					if ((classId > 224) && (classId < 240)) // D.Elf Assassins / High Elfs Mage
					{
						appearance.setFemale();
					}
					
					if (hasQuestItems(player, CLASS_CHANGE_COUPON))
					{
						takeItems(player, CLASS_CHANGE_COUPON, 1);
						
						player.stopAllEffects();
						player.getEffectList().stopAllToggles();
						player.getEffectList().stopAllEffectsWithoutExclusions(false, false); // ReplaceSkillBySkill should stop here.
						player.removeAllSkills();
						player.getEffectList().stopAllEffectsWithoutExclusions(false, false); // After removal of skills.
						
						player.setVitalityPoints(0, true);
						player.setExpBeforeDeath(0);
						
						if (female)
						{
							appearance.setFemale();
						}
						else
						{
							appearance.setMale();
						}
						player.setPlayerClass(classId);
						player.setBaseClass(player.getActiveClass());
						
						giveItems(player, SP_SCROLL, 50);
						if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
						{
							player.getVariables().set("3rdClassMountRewarded", true);
							if (player.isDeathKnight())
							{
								giveItems(player, SPELLBOOK_DEATH_KNIGHT, 1);
							}
							else
							{
								switch (player.getRace())
								{
									case ELF:
									{
										giveItems(player, SPELLBOOK_ELF, 1);
										break;
									}
									case DARK_ELF:
									{
										giveItems(player, SPELLBOOK_DELF, 1);
										break;
									}
									case ORC:
									{
										if (!player.isVanguard())
										{
											giveItems(player, SPELLBOOK_ORC, 1);
										}
										break;
									}
									case DWARF:
									{
										giveItems(player, SPELLBOOK_DWARF, 1);
										break;
									}
									case KAMAEL:
									{
										giveItems(player, SPELLBOOK_KAMAEL, 1);
										break;
									}
									case HUMAN:
									{
										giveItems(player, SPELLBOOK_HUMAN, 1);
										break;
									}
									case SYLPH:
									{
										giveItems(player, SPELLBOOK_SYLPH, 1);
										break;
									}
								}
							}
						}
						
						player.getServitorsAndPets().forEach(s -> s.unSummon(player));
						player.getEffectList().stopAllEffects(true);
						player.getInventory().getItems().forEach(item ->
						{
							if (item.isEquipped())
							{
								player.getInventory().unEquipItemInSlot(item.getLocationSlot());
							}
						});
						
						// Appearance after class change.
						appearance.setHairStyle(0);
						appearance.setHairColor(0);
						appearance.setFace(0);
						
						// Remove olympiad nobless.
						Olympiad.removeNobleStats(player.getObjectId());
						
						player.store(false);
						player.broadcastUserInfo();
						player.sendSkillList();
						player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));
						player.sendPacket(new ExUserInfoInvenWeight(player));
						Disconnection.of(player).defaultSequence(LeaveWorld.STATIC_PACKET);
					}
					else
					{
						html = "34152-no_coupon.htm";
					}
				}
				break;
			}
		}
		return html;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	private String checkConditions(Npc npc, Player player)
	{
		if (!hasQuestItems(player, CLASS_CHANGE_COUPON))
		{
			return npc.getId() + "-no_coupon.htm";
		}
		for (Item item : player.getInventory().getItems())
		{
			if (item.isEquipped())
			{
				return npc.getId() + "-unequip_items.htm";
			}
		}
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() >= 3))
		{
			return npc.getId() + "-no_inventory.htm";
		}
		if (!player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			return npc.getId() + "-3rd_class.htm";
		}
		if (player.isInOlympiadMode() || player.isHero() || Hero.getInstance().isHero(player.getObjectId()) || Hero.getInstance().isUnclaimedHero(player.getObjectId()))
		{
			return npc.getId() + "-1.htm";
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new AdditionalServicesAdvisor();
	}
}
