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
package ai.areas.DwellingOfSpiritsResidence;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

public class ResidenceOfKingPetram extends AbstractNpcAI
{
	// NPCs
	private static final int PETRAM = 29108;
	private static final int PETRAM_PIECE = 29116;
	private static final int PETRAM_FRAGMENT = 29117;
	// Skills
	private static final SkillHolder EARTH_ENERGY = new SkillHolder(50066, 1);
	private static final SkillHolder EARTH_FURY = new SkillHolder(50059, 1);
	private static final SkillHolder TEST = new SkillHolder(5712, 1);
	
	public ResidenceOfKingPetram()
	{
		addKillId(PETRAM_PIECE, PETRAM_FRAGMENT);
		addAttackId(PETRAM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SPAWN_MINION":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					final Npc petram = world.getNpc(PETRAM);
					petram.doCast(EARTH_ENERGY.getSkill());
					
					petram.setInvul(true);
					petram.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.EARTH_KING_BARRIER2_AVE);
					petram.updateAbnormalVisualEffects();
					petram.broadcastSay(ChatType.NPC_SHOUT, "HaHa, fighters lets kill them. Now Im invul!!!");
					
					final int stage = getHigherStage(world);
					for (int minionIndex = 0; minionIndex < StageData.getMinionsStageData(stage).length; minionIndex++)
					{
						final MinionData minionData = StageData.getMinionsStageData(stage)[minionIndex];
						final Location minionLocation = minionData.getMinionLocation();
						world.setParameter("minion" + minionIndex, addSpawn(npc, minionData.getMinionId(), minionLocation.getX(), minionLocation.getY(), minionLocation.getZ(), minionLocation.getHeading(), false, -1, true, npc.getInstanceId()));
					}
					
					startQuestTimer("SUPPORT_PETRAM", 200, npc, null);
				}
				break;
			}
			case "SUPPORT_PETRAM":
			{
				final Instance world = npc.getInstanceWorld();
				if ((world != null) && (world.getNpc(PETRAM) != null) && !world.getNpc(PETRAM).isDead())
				{
					final int stage = getHigherStage(world);
					for (int minionIndex = 0; minionIndex < StageData.getMinionsStageData(stage).length; minionIndex++)
					{
						world.getParameters().getObject("minion" + minionIndex, Npc.class).setTarget(world.getNpc(PETRAM));
						world.getParameters().getObject("minion" + minionIndex, Npc.class).doCast(TEST.getSkill());
					}
					startQuestTimer("SUPPORT_PETRAM", 10100, npc, null);
				}
				break;
			}
			case "REMOVE_INVUL":
			{
				if (npc != null)
				{
					final Instance world = npc.getInstanceWorld();
					if (world != null)
					{
						final Npc petram = world.getNpc(PETRAM);
						if (petram != null)
						{
							petram.doCast(EARTH_FURY.getSkill());
							petram.setInvul(false);
							petram.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.EARTH_KING_BARRIER2_AVE);
							petram.updateAbnormalVisualEffects();
							petram.broadcastSay(ChatType.NPC_SHOUT, "Nooooo... Nooooo...");
						}
						
						for (int i = 0; i < 12; i++)
						{
							final Npc minion = world.getParameters().getObject("minion" + i, Npc.class);
							if (minion != null)
							{
								minion.deleteMe();
							}
						}
					}
				}
				break;
			}
		}
		
		return null;
	}
	
	private int getHigherStage(Instance world)
	{
		for (int stage = StageData.getMinionsStageData().size() - 1; stage >= 0; stage--)
		{
			if (world.getParameters().getBoolean("stage" + stage, false))
			{
				return stage;
			}
		}
		
		return 0;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance world = npc.getInstanceWorld();
		synchronized (this)
		{
			for (int stage = 0; stage < StageData.getMinionsStageData().size(); stage++)
			{
				if ((npc.getCurrentHpPercent() < StageData.getHpPercents()[stage]) && !world.getParameters().getBoolean("stage" + stage, false))
				{
					world.setParameter("stage" + stage, true);
					startQuestTimer("SPAWN_MINION", 100, npc, null);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		final int stage = getHigherStage(world);
		int aliveMinionsCount = 0;
		for (int minionIndex = 0; minionIndex < StageData.getMinionsStageData(stage).length; minionIndex++)
		{
			if ((world.getParameters().getObject("minion" + minionIndex, Npc.class) != null) && !world.getParameters().getObject("minion" + minionIndex, Npc.class).isDead())
			{
				aliveMinionsCount++;
			}
		}
		
		boolean breakInvul = false;
		switch (stage)
		{
			case 0:
			{
				if (aliveMinionsCount == 0)
				{
					breakInvul = true;
				}
				break;
			}
			case 1:
			{
				if (aliveMinionsCount <= 4)
				{
					breakInvul = true;
				}
				break;
			}
			case 2:
			{
				if (aliveMinionsCount <= 6)
				{
					breakInvul = true;
				}
				break;
			}
		}
		
		if (breakInvul)
		{
			startQuestTimer("REMOVE_INVUL", 500, world.getNpc(PETRAM), null);
		}
	}
	
	private static class StageData
	{
		private static final int[] _hpPercents = new int[]
		{
			75,
			50,
			10
		};
		private static final Map<Integer, MinionData[]> _minionsStageData = new HashMap<>();
		static
		{
			// Stage 1
			_minionsStageData.put(0, new MinionData[]
			{
				new MinionData(PETRAM_FRAGMENT, new Location(221543, 191530, -15486, 1131)),
				new MinionData(PETRAM_FRAGMENT, new Location(222069, 192019, -15486, 49364)),
				new MinionData(PETRAM_FRAGMENT, new Location(222595, 191479, -15486, 34013)),
				new MinionData(PETRAM_FRAGMENT, new Location(222077, 191017, -15486, 16383))
			});
			
			// Stage 2
			_minionsStageData.put(1, new MinionData[]
			{
				new MinionData(PETRAM_FRAGMENT, new Location(221069, 191544, -15486, 2280)),
				new MinionData(PETRAM_FRAGMENT, new Location(221366, 192223, -15486, 56731)),
				new MinionData(PETRAM_FRAGMENT, new Location(222067, 192508, -15486, 50632)),
				new MinionData(PETRAM_FRAGMENT, new Location(222765, 192216, -15486, 39607)),
				new MinionData(PETRAM_FRAGMENT, new Location(223057, 191472, -15486, 33154)),
				new MinionData(PETRAM_FRAGMENT, new Location(222773, 190814, -15486, 25376)),
				new MinionData(PETRAM_FRAGMENT, new Location(222063, 190516, -15486, 16383)),
				new MinionData(PETRAM_FRAGMENT, new Location(221342, 190800, -15486, 10837))
			});
			// Stage 3
			_minionsStageData.put(2, new MinionData[]
			{
				new MinionData(PETRAM_PIECE, new Location(221543, 191530, -15486, 1131)),
				new MinionData(PETRAM_PIECE, new Location(222069, 192019, -15486, 49364)),
				new MinionData(PETRAM_PIECE, new Location(222595, 191479, -15486, 34013)),
				new MinionData(PETRAM_PIECE, new Location(222077, 191017, -15486, 16383)),
				new MinionData(PETRAM_PIECE, new Location(221069, 191544, -15486, 2280)),
				new MinionData(PETRAM_PIECE, new Location(221366, 192223, -15486, 56731)),
				new MinionData(PETRAM_PIECE, new Location(222067, 192508, -15486, 50632)),
				new MinionData(PETRAM_PIECE, new Location(222765, 192216, -15486, 39607)),
				new MinionData(PETRAM_PIECE, new Location(223057, 191472, -15486, 33154)),
				new MinionData(PETRAM_PIECE, new Location(222773, 190814, -15486, 25376)),
				new MinionData(PETRAM_PIECE, new Location(222063, 190516, -15486, 16383)),
				new MinionData(PETRAM_PIECE, new Location(221342, 190800, -15486, 10837))
			});
		}
		
		public static int[] getHpPercents()
		{
			return _hpPercents;
		}
		
		public static Map<Integer, MinionData[]> getMinionsStageData()
		{
			return _minionsStageData;
		}
		
		public static MinionData[] getMinionsStageData(int stage)
		{
			return _minionsStageData.get(stage);
		}
	}
	
	private static class MinionData
	{
		final int _minionId;
		final Location _minionLocation;
		
		private MinionData(int minionId, Location minionLocation)
		{
			_minionId = minionId;
			_minionLocation = minionLocation;
		}
		
		public int getMinionId()
		{
			return _minionId;
		}
		
		public Location getMinionLocation()
		{
			return _minionLocation;
		}
	}
	
	public static void main(String[] args)
	{
		new ResidenceOfKingPetram();
	}
}
