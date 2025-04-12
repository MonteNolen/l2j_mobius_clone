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
package instances.DreamDungeon.VentusTemple;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;

import ai.AbstractNpcAI;

/**
 * @author Index
 */
public class BrokenBallista extends AbstractNpcAI
{
	private static final int BROKEN_BALLISTA_NPC_ID = 18675;
	
	private BrokenBallista()
	{
		addSpawnId(BROKEN_BALLISTA_NPC_ID);
		addFirstTalkId(BROKEN_BALLISTA_NPC_ID);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc == null ? null : npc.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != VentusTemple.INSTANCE_ID))
		{
			return;
		}
		
		if ((instance.getStatus() == VentusTemple.SHOOT_FROM_BALLISTA) && (npc != null))
		{
			npc.setRandomAnimation(true);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((player == null) || (npc == null))
		{
			return super.onFirstTalk(npc, player);
		}
		
		final Instance instance = player.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != VentusTemple.INSTANCE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		if (instance.getStatus() == VentusTemple.SHOOT_FROM_BALLISTA)
		{
			instance.setStatus(VentusTemple.BOSS_FIGHT);
			npc.setTargetable(false);
			player.setTarget(null);
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new BrokenBallista();
	}
}
