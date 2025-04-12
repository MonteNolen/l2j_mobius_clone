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
package instances.ValakasTemple.ValakasHermit;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;

import ai.AbstractNpcAI;
import instances.ValakasTemple.ValakasTemple;

/**
 * @author Index
 */
public class ValakasHermit extends AbstractNpcAI
{
	private static final int HERMIT_NPC_ID = 34337;
	
	private ValakasHermit()
	{
		addFirstTalkId(HERMIT_NPC_ID);
		addSpawnId(HERMIT_NPC_ID);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		final NpcSpawnTemplate spawn = npc.getSpawn().getNpcSpawnTemplate();
		final String group = spawn.getGroup().getName();
		final int groupId = Integer.parseInt(group.replaceAll("hermit_", ""));
		npc.setScriptValue(groupId);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		return HERMIT_NPC_ID + "-0" + npc.getScriptValue() + ".htm";
	}
	
	public static void main(String[] args)
	{
		new ValakasHermit();
	}
}
