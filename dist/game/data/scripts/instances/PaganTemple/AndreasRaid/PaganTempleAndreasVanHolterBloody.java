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
package instances.PaganTemple.AndreasRaid;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;
import instances.PaganTemple.PaganTempleManager;

/**
 * @author Index
 */
public class PaganTempleAndreasVanHolterBloody extends AbstractNpcAI
{
	private final static int ANDREAS_NPC_ID = 29212;
	private final static SkillHolder TEMPLE_THORNS_02 = new SkillHolder(48698, 2);
	private final static SkillHolder TEMPLE_THORNS_03 = new SkillHolder(48698, 3);
	private final static SkillHolder TIROL_ENSLAVE = new SkillHolder(48699, 3);
	private final static SkillHolder FORCE_SIT = new SkillHolder(48701, 2);
	private final static SkillHolder TRIOL_HOLD = new SkillHolder(48703, 1);
	
	public PaganTempleAndreasVanHolterBloody()
	{
		setInstanceStatusChangeId(this::onInstanceStatusChange, PaganTempleManager.INSTANCE_TEMPLATE_ID);
		addAttackId(ANDREAS_NPC_ID);
		addKillId(ANDREAS_NPC_ID);
		addSpellFinishedId(ANDREAS_NPC_ID);
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance world = event.getWorld();
		if (world == null)
		{
			return;
		}
		
		switch (world.getStatus())
		{
			case PaganTempleManager.NORMAL:
			case PaganTempleManager.CLOSED:
			case PaganTempleManager.ANDREAS_DEAD:
			{
				PaganTempleManager.deSpawnNpcGroup(world, "BLOODY_ADREAS");
				PaganTempleManager.deSpawnNpcGroup(world, "AKOLYTH_GUARD_01");
				PaganTempleManager.deSpawnNpcGroup(world, "AKOLYTH_GUARD_CENTER_LEFT");
				PaganTempleManager.deSpawnNpcGroup(world, "AKOLYTH_GUARD_CENTER_RIGHT");
				PaganTempleManager.deSpawnNpcGroup(world, "AKOLYTH_GUARD_FAR_LEFT");
				PaganTempleManager.deSpawnNpcGroup(world, "AKOLYTH_GUARD_FAR_RIGHT");
				break;
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance world = (attacker == null) || (npc == null) ? null : attacker.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			super.onAttack(npc, attacker, damage, isSummon, skill);
		}
		else if (!world.getParameters().contains(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK))
		{
			world.getParameters().set(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK, ThreadPool.scheduleAtFixedRate(() -> thinkAction(world, npc), 2_000, 2_000));
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final Instance world = (player == null) || (npc == null) ? null : player.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		if (!world.getParameters().contains(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK))
		{
			world.getParameters().set(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK, ThreadPool.scheduleAtFixedRate(() -> thinkAction(world, npc), 2_000, 2_000));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = (killer == null) || (npc == null) ? null : killer.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		world.setStatus(PaganTempleManager.ANDREAS_DEAD);
	}
	
	@Override
	public void onNpcDespawn(Npc npc)
	{
		super.onNpcDespawn(npc);
		final Instance world = (npc == null) ? null : npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		if (world.getParameters().contains(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK))
		{
			final ScheduledFuture<?> task = world.getParameters().getObject(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK, ScheduledFuture.class, null);
			if (task != null)
			{
				task.cancel(true);
			}
			world.getParameters().remove(PaganTempleManager.VARIABLE_ANDREAS_BOSS_THINK_TASK);
		}
	}
	
	public static void thinkAction(Instance world, Npc npc)
	{
		checkAndAddGuard(world, npc);
		if (!npc.isCastingNow() && !npc.isMovementDisabled())
		{
			if (Rnd.get(100_000) < 95_000)
			{
				final SkillHolder skill = Rnd.nextBoolean() ? TEMPLE_THORNS_02 : TEMPLE_THORNS_03;
				npc.doCast(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
			}
			else if (Rnd.get(100_000) < 45_000)
			{
				npc.doCast(SkillData.getInstance().getSkill(TIROL_ENSLAVE.getSkillId(), TIROL_ENSLAVE.getSkillLevel()));
			}
			else if (Rnd.get(100_000) < 25_000)
			{
				npc.doCast(SkillData.getInstance().getSkill(TRIOL_HOLD.getSkillId(), TRIOL_HOLD.getSkillLevel()));
			}
			else if (Rnd.get(100_000) < 5_000)
			{
				npc.doCast(SkillData.getInstance().getSkill(FORCE_SIT.getSkillId(), FORCE_SIT.getSkillLevel()));
			}
		}
	}
	
	private static void checkAndAddGuard(Instance world, Npc npc)
	{
		final int hpPercent = npc.getCurrentHpPercent();
		if ((hpPercent < 95.0) || (hpPercent < 90.0) || (hpPercent < 85.0) || (hpPercent < 80.0) || (hpPercent < 75.0) || (hpPercent < 70.0) || (hpPercent < 65.0) || (hpPercent < 60.0) || (hpPercent < 55.0) || (hpPercent < 50.0))
		{
			final List<Player> players = World.getInstance().getVisibleObjectsInRange(npc, Player.class, 600);
			final List<Npc> guardList01 = PaganTempleManager.spawnNpcsGroup(world, "AKOLYTH_GUARD_01", false, true);
			final List<Npc> guardList02 = PaganTempleManager.spawnNpcsGroup(world, "AKOLYTH_GUARD_CENTER_LEFT", false, true);
			final List<Npc> guardList03 = PaganTempleManager.spawnNpcsGroup(world, "AKOLYTH_GUARD_CENTER_RIGHT", false, true);
			final List<Npc> guardList04 = PaganTempleManager.spawnNpcsGroup(world, "AKOLYTH_GUARD_FAR_LEFT", false, true);
			final List<Npc> guardList05 = PaganTempleManager.spawnNpcsGroup(world, "AKOLYTH_GUARD_FAR_RIGHT", false, true);
			if ((guardList01 != null) && !players.isEmpty())
			{
				for (Npc guard : guardList01)
				{
					guard.setRunning();
					final Player player = players.get(Rnd.get(players.size()));
					guard.setTarget(player);
					guard.doAutoAttack(player);
				}
			}
			if ((guardList02 != null) && !players.isEmpty())
			{
				for (Npc guard : guardList02)
				{
					guard.setRunning();
					final Player player = players.get(Rnd.get(players.size()));
					guard.setTarget(player);
					guard.doAutoAttack(player);
				}
			}
			if ((guardList03 != null) && !players.isEmpty())
			{
				for (Npc guard : guardList03)
				{
					guard.setRunning();
					final Player player = players.get(Rnd.get(players.size()));
					guard.setTarget(player);
					guard.doAutoAttack(player);
				}
			}
			if ((guardList04 != null) && !players.isEmpty())
			{
				for (Npc guard : guardList04)
				{
					guard.setRunning();
					final Player player = players.get(Rnd.get(players.size()));
					guard.setTarget(player);
					guard.doAutoAttack(player);
				}
			}
			if ((guardList05 != null) && !players.isEmpty())
			{
				for (Npc guard : guardList05)
				{
					guard.setRunning();
					final Player player = players.get(Rnd.get(players.size()));
					guard.setTarget(player);
					guard.doAutoAttack(player);
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new PaganTempleAndreasVanHolterBloody();
	}
}
