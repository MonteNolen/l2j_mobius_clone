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
package ai.bosses;

import java.util.Calendar;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.DBSpawnManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * @URL https://l2central.info/essence/articles/23.html?lang=en
 * @author NasSeKa
 */
public final class ChaoticBosses extends AbstractNpcAI
{
	private static final int[] RAID_BOSSES =
	{
		29170, // Chaotic Core
		29171, // Chaotic Orfen
		29172, // Chaotic Queen Ant
		29173, // Chaotic Zaken
	};
	
	private ChaoticBosses()
	{
		addKillId(RAID_BOSSES);
		
		// Schedule reset everyday at 20:00.
		final long currentTime = System.currentTimeMillis();
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 20);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		// Daily reset task.
		final long calendarTime = calendar.getTimeInMillis();
		final long startDelay = Math.max(0, calendarTime - currentTime);
		ThreadPool.scheduleAtFixedRate(this::onSpawn, startDelay, 86400000); // 86400000 = 1 day
	}
	
	private void onSpawn()
	{
		for (int npcId : RAID_BOSSES)
		{
			for (Spawn spawn : SpawnTable.getInstance().getSpawns(npcId))
			{
				for (Npc monster : spawn.getSpawnedNpcs())
				{
					if (!monster.isAlikeDead())
					{
						DBSpawnManager.getInstance().deleteSpawn(spawn, true);
						monster.deleteMe();
					}
				}
			}
		}
		
		final int chaosBossId = RAID_BOSSES[getRandom(0, 3)];
		final NpcTemplate template = NpcData.getInstance().getTemplate(chaosBossId);
		try
		{
			if (template != null)
			{
				final Spawn spawn = new Spawn(template);
				spawn.setXYZ(191512, 21855, -3680);
				spawn.setRespawnDelay(86400000);
				DBSpawnManager.getInstance().addNewSpawn(spawn, true);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Caused an exception " + e.getMessage());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (ArrayUtil.contains(RAID_BOSSES, npc.getId()))
		{
			for (int npcId : RAID_BOSSES)
			{
				for (Spawn spawn : SpawnTable.getInstance().getSpawns(npcId))
				{
					for (Npc monster : spawn.getSpawnedNpcs())
					{
						if (!monster.isAlikeDead())
						{
							DBSpawnManager.getInstance().deleteSpawn(spawn, true);
							monster.deleteMe();
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new ChaoticBosses();
	}
}
