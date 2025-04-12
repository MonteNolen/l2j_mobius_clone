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
package ai.areas.FrostCastleZone;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;

/**
 * @author Serenitty
 */
public class FrostLordAvailability
{
	private static final Logger LOGGER = Logger.getLogger(FrostLordAvailability.class.getName());
	
	private static final int[] DAYS_OF_WEEK =
	{
		Calendar.TUESDAY,
		Calendar.THURSDAY
	};
	private static final int[] ACTIVATION_TIME =
	{
		18,
		0
	};
	private static final int[] DEACTIVATION_TIME =
	{
		11,
		59
	};
	private static final long ONE_DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);
	
	public FrostLordAvailability()
	{
		frostLordCastleActivation(DAYS_OF_WEEK, ACTIVATION_TIME);
		frostLordCastleDeactivation(DAYS_OF_WEEK, DEACTIVATION_TIME);
	}
	
	private void frostLordCastleActivation(int[] daysOfWeek, int[] time)
	{
		for (int dayOfWeek : daysOfWeek)
		{
			final long initialDelay = getNextDelay(dayOfWeek, time[0], time[1]);
			final long period = ONE_DAY_IN_MILLIS * 7;
			ThreadPool.scheduleAtFixedRate(this::enableFrostLord, initialDelay, period);
		}
	}
	
	private void frostLordCastleDeactivation(int[] daysOfWeek, int[] time)
	{
		for (int dayOfWeek : daysOfWeek)
		{
			final long initialDelay = getNextDelay(dayOfWeek, time[0], time[1]);
			final long period = ONE_DAY_IN_MILLIS * 7;
			ThreadPool.scheduleAtFixedRate(this::disableFrostLord, initialDelay, period);
		}
	}
	
	private long getNextDelay(int dayOfWeek, int hour, int minute)
	{
		final Calendar now = Calendar.getInstance();
		final int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
		final int daysUntilNextActivation = ((dayOfWeek + 7) - currentDayOfWeek) % 7;
		
		final Calendar activationTime = Calendar.getInstance();
		activationTime.add(Calendar.DAY_OF_YEAR, daysUntilNextActivation);
		activationTime.set(Calendar.HOUR_OF_DAY, hour);
		activationTime.set(Calendar.MINUTE, minute);
		activationTime.set(Calendar.SECOND, 0);
		
		if (activationTime.getTimeInMillis() < now.getTimeInMillis())
		{
			activationTime.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		return activationTime.getTimeInMillis() - now.getTimeInMillis();
	}
	
	private void enableFrostLord()
	{
		GlobalVariablesManager.getInstance().set("AvailableFrostLord", true);
		LOGGER.info("Frost Lord enabled.");
	}
	
	private void disableFrostLord()
	{
		GlobalVariablesManager.getInstance().set("AvailableFrostLord", false);
		LOGGER.info("Frost Lord disabled.");
	}
	
	public static void main(String[] args)
	{
		new FrostLordAvailability();
	}
}
