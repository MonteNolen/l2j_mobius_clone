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
package quests.Q10038_NewLifesLessons;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestDialogType;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.model.quest.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestDialog;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;

import quests.Q10039_StrengthOfSpirit.Q10039_StrengthOfSpirit;
import quests.Q10040_StrengthOfSpirit.Q10040_StrengthOfSpirit;

/**
 * @author Magik, Mobius
 */
public class Q10038_NewLifesLessons extends Quest
{
	private static final int QUEST_ID = 10038;
	private static final int[] MONSTERS =
	{
		18342, // Gremlin (this is used for now)
		20001, // Gremlin
	};
	
	public Q10038_NewLifesLessons()
	{
		super(QUEST_ID);
		addKillId(MONSTERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ACCEPT":
			{
				if (!canStartQuest(player))
				{
					break;
				}
				
				final QuestState questState = getQuestState(player, true);
				if (!questState.isStarted() && !questState.isCompleted())
				{
					questState.startQuest();
				}
				break;
			}
			case "TELEPORT":
			{
				QuestState questState = getQuestState(player, false);
				if (questState == null)
				{
					if (!canStartQuest(player))
					{
						break;
					}
					
					questState = getQuestState(player, true);
					
					final NewQuestLocation questLocation = getQuestData().getLocation();
					if (questLocation.getStartLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getStartLocationId()).getLocation();
						if (teleportToQuestLocation(player, location))
						{
							questState.setCond(QuestCondType.ACT);
							sendAcceptDialog(player);
						}
					}
					break;
				}
				
				final NewQuestLocation questLocation = getQuestData().getLocation();
				if (questState.isCond(QuestCondType.STARTED))
				{
					if (questLocation.getQuestLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getQuestLocationId()).getLocation();
						if (teleportToQuestLocation(player, location) && (questLocation.getQuestLocationId() == questLocation.getEndLocationId()))
						{
							questState.setCond(QuestCondType.DONE);
							sendEndDialog(player);
						}
					}
				}
				else if (questState.isCond(QuestCondType.DONE) && !questState.isCompleted())
				{
					if (questLocation.getEndLocationId() > 0)
					{
						final Location location = TeleportListData.getInstance().getTeleport(questLocation.getEndLocationId()).getLocation();
						if (teleportToQuestLocation(player, location))
						{
							sendEndDialog(player);
						}
					}
				}
				break;
			}
			case "COMPLETE":
			{
				final QuestState questState = getQuestState(player, false);
				if (questState == null)
				{
					break;
				}
				
				if (questState.isCond(QuestCondType.DONE) && !questState.isCompleted())
				{
					questState.exitQuest(false, true);
					rewardPlayer(player);
					
					Quest quest = QuestManager.getInstance().getQuest(10039);
					if (quest.canStartQuest(player))
					{
						final QuestState nextQuestState = player.getQuestState(Q10039_StrengthOfSpirit.class.getSimpleName());
						if (nextQuestState == null)
						{
							player.sendPacket(new ExQuestDialog(10039, QuestDialogType.ACCEPT));
						}
					}
					else
					{
						quest = QuestManager.getInstance().getQuest(10040);
						if (quest.canStartQuest(player))
						{
							final QuestState nextQuestState = player.getQuestState(Q10040_StrengthOfSpirit.class.getSimpleName());
							if (nextQuestState == null)
							{
								player.sendPacket(new ExQuestDialog(10040, QuestDialogType.ACCEPT));
							}
						}
					}
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState questState = getQuestState(player, false);
		if ((questState != null) && !questState.isCompleted())
		{
			if (questState.isCond(QuestCondType.NONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.START));
			}
			else if (questState.isCond(QuestCondType.DONE))
			{
				player.sendPacket(new ExQuestDialog(QUEST_ID, QuestDialogType.END));
			}
		}
		
		npc.showChatWindow(player);
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState questState = getQuestState(killer, false);
		if ((questState != null) && questState.isCond(QuestCondType.STARTED))
		{
			questState.setCount(getQuestData().getGoal().getCount());
			questState.setCond(QuestCondType.DONE);
			killer.sendPacket(new ExQuestNotification(questState));
		}
	}
}
