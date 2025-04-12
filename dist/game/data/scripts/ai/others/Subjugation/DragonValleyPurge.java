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
package ai.others.Subjugation;

import java.util.Calendar;

import org.l2jmobius.gameserver.data.holders.SubjugationHolder;
import org.l2jmobius.gameserver.data.xml.SubjugationData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerPurgeHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.subjugation.ExSubjugationSidebar;

import ai.AbstractNpcAI;

/**
 * @author Berezkin Nikolay, Serenitty
 */
public class DragonValleyPurge extends AbstractNpcAI
{
	private static final int CATEGORY = 5;
	private static final int MAX_KEYS = 40;
	private static final int PURGE_MAX_POINT = 1000000;
	private static final SubjugationHolder PURGE_DATA = SubjugationData.getInstance().getSubjugation(CATEGORY);
	
	private DragonValleyPurge()
	{
		addKillId(PURGE_DATA.getNpcs().keySet().stream().mapToInt(it -> it).toArray());
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer.getVitalityPoints() > 0)
		{
			boolean isHotTime = false;
			for (int[] it : SubjugationData.getInstance().getSubjugation(CATEGORY).getHottimes())
			{
				final int minHour = it[0];
				final int maxHour = it[1];
				final int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				if ((currentHour >= minHour) && (maxHour > currentHour))
				{
					isHotTime = true;
				}
			}
			final int pointsForMob = isHotTime ? PURGE_DATA.getNpcs().get(npc.getId()) * 2 : PURGE_DATA.getNpcs().get(npc.getId());
			final int currentPurgePoints = (killer.getPurgePoints().get(CATEGORY) == null) ? 0 : killer.getPurgePoints().get(CATEGORY).getPoints();
			final int currentKeys = (killer.getPurgePoints().get(CATEGORY) == null) ? 0 : killer.getPurgePoints().get(CATEGORY).getKeys();
			final int remainingKeys = (killer.getPurgePoints().get(CATEGORY) == null) ? 0 : killer.getPurgePoints().get(CATEGORY).getRemainingKeys();
			killer.getPurgePoints().put(CATEGORY, new PlayerPurgeHolder(Math.min(PURGE_MAX_POINT, currentPurgePoints + pointsForMob), currentKeys, remainingKeys));
			lastCategory(killer);
			checkPurgeComplete(killer);
			killer.sendPacket(new ExSubjugationSidebar(killer, killer.getPurgePoints().get(CATEGORY)));
		}
	}
	
	private void checkPurgeComplete(Player player)
	{
		final int points = player.getPurgePoints().get(CATEGORY).getPoints();
		final int keys = player.getPurgePoints().get(CATEGORY).getKeys();
		if ((points >= PURGE_MAX_POINT) && (keys < MAX_KEYS))
		{
			player.getPurgePoints().put(CATEGORY, new PlayerPurgeHolder(points - PURGE_MAX_POINT, keys + 1, player.getPurgePoints().get(CATEGORY).getRemainingKeys() - 1));
		}
	}
	
	private void lastCategory(Player player)
	{
		if (player.getPurgeLastCategory() == CATEGORY)
		{
			return;
		}
		player.getVariables().remove(PlayerVariables.PURGE_LAST_CATEGORY);
		player.getVariables().set(PlayerVariables.PURGE_LAST_CATEGORY, CATEGORY);
	}
	
	public static void main(String[] args)
	{
		new DragonValleyPurge();
	}
}
