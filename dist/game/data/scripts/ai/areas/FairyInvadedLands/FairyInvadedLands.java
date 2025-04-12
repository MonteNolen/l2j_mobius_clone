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
package ai.areas.FairyInvadedLands;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.Broadcast;

import ai.AbstractNpcAI;

/**
 * @author Liamxroy
 * @URL https://l2central.info/essence/articles/540.html?lang=en
 */
public class FairyInvadedLands extends AbstractNpcAI
{
	private static final Logger LOGGER = Logger.getLogger(FairyInvadedLands.class.getName());
	
	private static final int LYCARIA = 22800;
	private static final int LYANSUS = 22802;
	private static final int[] MONSTERS =
	{
		22789,
		22790,
		22791,
		22792,
		22793,
		22794,
		22795,
		22796,
		22797,
		22798,
		22799,
		22801
	};
	private static final SpawnTemplate LYCARIA_SPAWN = SpawnData.getInstance().getSpawnByName("Lycaria");
	private static final SpawnTemplate LYANSUS_SPAWN = SpawnData.getInstance().getSpawnByName("Lyansus");
	private static final AtomicInteger KILL_COUNTER = new AtomicInteger();
	private static boolean LYCARIA_SPAWNED = false;
	private static boolean LYANSUS_SPAWNED = false;
	private int[] _spawnHours;
	
	public FairyInvadedLands()
	{
		addKillId(LYCARIA, LYANSUS);
		addKillId(MONSTERS);
		_spawnHours = generateSpawnHours();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "announce_lycaria":
			{
				Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.THE_FAIRY_MAGIC_HAS_ATTRACTED_LYCARIA, 2, 10000, true));
				break;
			}
			case "announce_lyansus":
			{
				Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.THE_FAIRY_MAGIC_HAS_ATTRACTED_LYANSUS, 2, 10000, true));
				break;
			}
			case "spawn":
			{
				if (shouldSpawnNow())
				{
					if (!LYCARIA_SPAWNED)
					{
						LYCARIA_SPAWNED = true;
						KILL_COUNTER.set(0);
						LOGGER.info("Fairy Invaded Lands: Spawns Lycaria.");
						
						LYCARIA_SPAWN.getGroups().forEach(SpawnGroup::spawnAll);
						startQuestTimer("announce_lycaria", 1000, null, null);
						startQuestTimer("despawn_lycaria", 3600000, null, null); // Despawn after 1 hour.
						startQuestTimer("lycaria_despawned", 3600001, null, null); // Set Available for spawn after 1 hour.
					}
					if (!LYANSUS_SPAWNED)
					{
						LYANSUS_SPAWNED = true;
						LOGGER.info("Fairy Invaded Lands: Spawns Lyansus.");
						startQuestTimer("announce_lyansus", 15000, null, null);
						LYANSUS_SPAWN.getGroups().forEach(SpawnGroup::spawnAll);
						startQuestTimer("despawn_lyansus", 3600000, null, null); // Despawn after 1 hour.
						startQuestTimer("lyansus_despawned", 3600001, null, null); // Set Available for spawn after 1 hour.
					}
				}
				break;
			}
			case "despawn_lycaria":
			{
				KILL_COUNTER.set(0);
				LYCARIA_SPAWN.getGroups().forEach(SpawnGroup::despawnAll);
				break;
			}
			case "despawn_lyansus":
			{
				KILL_COUNTER.set(0);
				LYANSUS_SPAWN.getGroups().forEach(SpawnGroup::despawnAll);
				break;
			}
			case "lycaria_despawned":
			{
				LYCARIA_SPAWNED = false;
				_spawnHours = generateSpawnHours(); // Regenerate spawn hours.
				break;
			}
			case "lyansus_despawned":
			{
				LYANSUS_SPAWNED = false;
				_spawnHours = generateSpawnHours(); // Regenerate spawn hours.
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int count = KILL_COUNTER.incrementAndGet();
		killer.sendMessage("Killer count: " + count + " /500");
		if (count >= 500)
		{
			startQuestTimer("spawn", 1000, null, null);
		}
		
		if (npc.getId() == LYCARIA)
		{
			Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.S1_DEFEATS_GUARDIAN_OF_THE_INVADED_LANDS, 2, 10000, true, killer.getName()));
			startQuestTimer("despawn_lycaria", 1000, null, null);
		}
		else if (npc.getId() == LYANSUS)
		{
			Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.S1_DEFEATS_GUARDIAN_OF_THE_INVADED_LANDS, 2, 10000, true, killer.getName()));
			startQuestTimer("despawn_lyansus", 1000, null, null);
		}
	}
	
	private int[] generateSpawnHours()
	{
		final int[] hours = new int[3];
		hours[0] = getRandom(24);
		hours[1] = getRandom(24);
		hours[2] = getRandom(24);
		LOGGER.info("Fairy Invaded Lands: Randomly spawns at " + hours[0] + ":**, " + hours[1] + ":** and " + hours[2] + ":**");
		return hours;
	}
	
	private boolean shouldSpawnNow()
	{
		final Calendar calendar = Calendar.getInstance();
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		for (int spawnHour : _spawnHours)
		{
			if (hour == spawnHour)
			{
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new FairyInvadedLands();
	}
}
