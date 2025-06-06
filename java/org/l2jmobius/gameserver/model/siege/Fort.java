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
package org.l2jmobius.gameserver.model.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.data.xml.StaticObjectData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.StaticObject;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.residences.AbstractResidence;
import org.l2jmobius.gameserver.model.zone.type.FortZone;
import org.l2jmobius.gameserver.model.zone.type.SiegeZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Fort extends AbstractResidence
{
	protected static final Logger LOGGER = Logger.getLogger(Fort.class.getName());
	
	private final List<Door> _doors = new ArrayList<>();
	private StaticObject _flagPole = null;
	private FortSiege _siege = null;
	private Calendar _siegeDate;
	private Calendar _lastOwnedTime;
	private SiegeZone _zone;
	private Clan _fortOwner = null;
	private int _fortType = 0;
	private int _state = 0;
	private int _castleId = 0;
	private int _supplyLeveL = 0;
	private final Map<Integer, FortFunction> _function = new ConcurrentHashMap<>();
	private final ScheduledFuture<?>[] _fortUpdater = new ScheduledFuture<?>[2];
	
	// Spawn Data
	private boolean _isSuspiciousMerchantSpawned = false;
	private final Set<Spawn> _siegeNpcs = ConcurrentHashMap.newKeySet();
	private final Set<Spawn> _npcCommanders = ConcurrentHashMap.newKeySet();
	private final Set<Spawn> _specialEnvoys = ConcurrentHashMap.newKeySet();
	
	private final Map<Integer, Integer> _envoyCastles = new HashMap<>(2);
	private final Set<Integer> _availableCastles = new HashSet<>(1);
	
	/** Fortress Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public class FortFunction
	{
		final int _type;
		private int _level;
		protected int _fee;
		protected int _tempFee;
		final long _rate;
		long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public FortFunction(int type, int level, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_level = level;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLevel()
		{
			return _level;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLevel(int level)
		{
			_level = level;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (_fortOwner == null)
			{
				return;
			}
			final long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
			{
				ThreadPool.schedule(new FunctionTask(cwh), _endDate - currentTime);
			}
			else
			{
				ThreadPool.schedule(new FunctionTask(cwh), 0);
			}
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			@Override
			public void run()
			{
				try
				{
					if (_fortOwner == null)
					{
						return;
					}
					if ((_fortOwner.getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						final int fee = _endDate == -1 ? _tempFee : _fee;
						setEndTime(System.currentTimeMillis() + _rate);
						dbSave();
						if (_cwh)
						{
							_fortOwner.getWarehouse().destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, fee, null, null);
						}
						ThreadPool.schedule(new FunctionTask(true), _rate);
					}
					else
					{
						removeFunction(_type);
					}
				}
				catch (Throwable t)
				{
					// Ignore.
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO fort_functions (fort_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)"))
			{
				ps.setInt(1, getResidenceId());
				ps.setInt(2, _type);
				ps.setInt(3, _level);
				ps.setInt(4, _fee);
				ps.setLong(5, _rate);
				ps.setLong(6, _endDate);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Exception: Fort.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
	
	public Fort(int fortId)
	{
		super(fortId);
		load();
		loadFlagPoles();
		if (_fortOwner != null)
		{
			setVisibleFlag(true);
			loadFunctions();
		}
		initResidenceZone();
		// initFunctions();
		initNpcs(); // load and spawn npcs (Always spawned)
		initSiegeNpcs(); // load suspicious merchants (Despawned 10mins before siege)
		// spawnSuspiciousMerchant(); // spawn suspicious merchants
		initNpcCommanders(); // npc Commanders (not monsters) (Spawned during siege)
		spawnNpcCommanders(); // spawn npc Commanders
		initSpecialEnvoys(); // envoys from castles (Spawned after fort taken)
		if ((_fortOwner != null) && (_state == 0))
		{
			spawnSpecialEnvoys();
		}
	}
	
	/**
	 * Return function with id
	 * @param type
	 * @return
	 */
	public FortFunction getFortFunction(int type)
	{
		return _function.get(type);
	}
	
	public void endOfSiege(Clan clan)
	{
		ThreadPool.execute(new endFortressSiege(this, clan));
	}
	
	/**
	 * Move non clan members off fort area and to nearest town.
	 */
	public void banishForeigners()
	{
		getResidenceZone().banishForeigners(_fortOwner.getId());
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		final SiegeZone zone = getZone();
		return (zone != null) && zone.isInsideZone(x, y, z);
	}
	
	public SiegeZone getZone()
	{
		if (_zone == null)
		{
			for (SiegeZone zone : ZoneManager.getInstance().getAllZones(SiegeZone.class))
			{
				if (zone.getSiegeObjectId() == getResidenceId())
				{
					_zone = zone;
					break;
				}
			}
		}
		return _zone;
	}
	
	@Override
	public FortZone getResidenceZone()
	{
		return (FortZone) super.getResidenceZone();
	}
	
	/**
	 * Get the objects distance to this fort
	 * @param obj
	 * @return
	 */
	public double getDistance(WorldObject obj)
	{
		return getZone().getDistanceToZone(obj);
	}
	
	public void closeDoor(Player player, int doorId)
	{
		openCloseDoor(player, doorId, false);
	}
	
	public void openDoor(Player player, int doorId)
	{
		openCloseDoor(player, doorId, true);
	}
	
	public void openCloseDoor(Player player, int doorId, boolean open)
	{
		if (player.getClan() != _fortOwner)
		{
			return;
		}
		
		final Door door = getDoor(doorId);
		if (door != null)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}
	
	// This method is used to begin removing all fort upgrades
	public void removeUpgrade()
	{
		removeDoorUpgrade();
	}
	
	/**
	 * This method will set owner for Fort
	 * @param clan
	 * @param updateClansReputation
	 * @return
	 */
	public boolean setOwner(Clan clan, boolean updateClansReputation)
	{
		if (clan == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Updating Fort owner with null clan!!!");
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED);
		sm.addCastleId(getResidenceId());
		getSiege().announceToPlayer(sm);
		
		final Clan oldowner = _fortOwner;
		if ((oldowner != null) && (clan != oldowner))
		{
			// Remove points from old owner
			updateClansReputation(oldowner, true);
			try
			{
				final Player oldleader = oldowner.getLeader().getPlayer();
				if ((oldleader != null) && (oldleader.getMountType() == MountType.WYVERN))
				{
					oldleader.dismount();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in setOwner: " + e.getMessage(), e);
			}
			if (getSiege().isInProgress())
			{
				getSiege().updatePlayerSiegeStateFlags(true);
			}
			removeOwner(true);
		}
		setFortState(0, 0); // initialize fort state
		
		// if clan already have castle, don't store him in fortress
		if (clan.getCastleId() > 0)
		{
			getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_REBEL_ARMY_RECAPTURED_THE_FORTRESS));
			return false;
		}
		
		// Give points to new owner
		if (updateClansReputation)
		{
			updateClansReputation(clan, false);
		}
		
		spawnSpecialEnvoys();
		// if clan have already fortress, remove it
		if (clan.getFortId() > 0)
		{
			FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
		}
		
		setSupplyLeveL(0);
		setOwnerClan(clan);
		updateOwnerInDB(); // Update in database
		saveFortVariables();
		
		if (getSiege().isInProgress())
		{
			getSiege().endSiege();
		}
		
		for (Player member : clan.getOnlineMembers(0))
		{
			giveResidentialSkills(member);
			member.sendSkillList();
		}
		return true;
	}
	
	public void removeOwner(boolean updateDB)
	{
		final Clan clan = _fortOwner;
		if (clan != null)
		{
			for (Player member : clan.getOnlineMembers(0))
			{
				removeResidentialSkills(member);
				member.sendSkillList();
			}
			clan.setFortId(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			setOwnerClan(null);
			setSupplyLeveL(0);
			saveFortVariables();
			removeAllFunctions();
			if (updateDB)
			{
				updateOwnerInDB();
			}
		}
	}
	
	public void raiseSupplyLeveL()
	{
		_supplyLeveL++;
		if (_supplyLeveL > Config.FS_MAX_SUPPLY_LEVEL)
		{
			_supplyLeveL = Config.FS_MAX_SUPPLY_LEVEL;
		}
	}
	
	public void setSupplyLeveL(int value)
	{
		if (value <= Config.FS_MAX_SUPPLY_LEVEL)
		{
			_supplyLeveL = value;
		}
	}
	
	public int getSupplyLeveL()
	{
		return _supplyLeveL;
	}
	
	public void saveFortVariables()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET supplyLvL=? WHERE id = ?"))
		{
			ps.setInt(1, _supplyLeveL);
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: saveFortVariables(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Show or hide flag inside flag pole.
	 * @param value
	 */
	public void setVisibleFlag(boolean value)
	{
		final StaticObject flagPole = _flagPole;
		if (flagPole != null)
		{
			flagPole.setMeshIndex(value ? 1 : 0);
		}
	}
	
	/**
	 * Respawn all doors on fort grounds.
	 */
	public void resetDoors()
	{
		for (Door door : _doors)
		{
			if (door.isOpen())
			{
				door.closeMe();
			}
			// Orc Fortress
			if (!door.isOpen())
			{
				door.openMe();
			}
			if (door.isDead())
			{
				door.doRevive();
			}
			if (door.getCurrentHp() < door.getMaxHp())
			{
				door.setCurrentHp(door.getMaxHp());
			}
		}
		loadDoorUpgrade(); // Check for any upgrade the doors may have
	}
	
	public void openOrcFortressDoors()
	{
		DoorData.getInstance().getDoor(23170012).openMe();
	}
	
	public void closeOrcFortressDoors()
	{
		DoorData.getInstance().getDoor(23170012).closeMe();
	}
	
	public void setOrcFortressOwnerNpcs(boolean val)
	{
		SpawnData.getInstance().getSpawns().forEach(spawnTemplate -> spawnTemplate.getGroupsByName("orc_fortress_owner_npcs").forEach(holder ->
		{
			if (val)
			{
				holder.spawnAll();
			}
			else
			{
				holder.despawnAll();
			}
		}));
	}
	
	// This method upgrade door
	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
		final Door door = getDoor(doorId);
		if (door != null)
		{
			door.setCurrentHp(door.getMaxHp() + hp);
			saveDoorUpgrade(doorId, hp, pDef, mDef);
		}
	}
	
	// This method loads fort
	@Override
	protected void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort WHERE id = ?"))
		{
			ps.setInt(1, getResidenceId());
			int ownerId = 0;
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					setName(rs.getString("name"));
					
					_siegeDate = Calendar.getInstance();
					_lastOwnedTime = Calendar.getInstance();
					_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
					_lastOwnedTime.setTimeInMillis(rs.getLong("lastOwnedTime"));
					ownerId = rs.getInt("owner");
					_fortType = rs.getInt("fortType");
					_state = rs.getInt("state");
					_castleId = rs.getInt("castleId");
					_supplyLeveL = rs.getInt("supplyLvL");
				}
			}
			if (ownerId > 0)
			{
				final Clan clan = ClanTable.getInstance().getClan(ownerId); // Try to find clan instance
				clan.setFortId(getResidenceId());
				setOwnerClan(clan);
				final int runCount = getOwnedTime() / (Config.FS_UPDATE_FRQ * 60);
				long initial = System.currentTimeMillis() - _lastOwnedTime.getTimeInMillis();
				while (initial > (Config.FS_UPDATE_FRQ * 60000))
				{
					initial -= Config.FS_UPDATE_FRQ * 60000;
				}
				initial = (Config.FS_UPDATE_FRQ * 60000) - initial;
				if ((Config.FS_MAX_OWN_TIME <= 0) || (getOwnedTime() < (Config.FS_MAX_OWN_TIME * 3600)))
				{
					_fortUpdater[0] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, runCount, FortUpdaterType.PERIODIC_UPDATE), initial, Config.FS_UPDATE_FRQ * 60000); // Schedule owner tasks to start running
					if (Config.FS_MAX_OWN_TIME > 0)
					{
						_fortUpdater[1] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, runCount, FortUpdaterType.MAX_OWN_TIME), 3600000, 3600000); // Schedule owner tasks to remove owener
					}
				}
				else
				{
					_fortUpdater[1] = ThreadPool.schedule(new FortUpdater(this, clan, 0, FortUpdaterType.MAX_OWN_TIME), 60000); // Schedule owner tasks to remove owner
				}
			}
			else
			{
				setOwnerClan(null);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + e.getMessage(), e);
		}
	}
	
	/** Load All Functions */
	private void loadFunctions()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_functions WHERE fort_id = ?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_function.put(rs.getInt("type"), new FortFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Fort.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove function In List and in DB
	 * @param functionType
	 */
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM fort_functions WHERE fort_id=? AND type=?"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, functionType);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception: Fort.removeFunctions(int functionType): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove all fort functions.
	 */
	private void removeAllFunctions()
	{
		for (int id : _function.keySet())
		{
			removeFunction(id);
		}
	}
	
	public boolean updateFunctions(Player player, int type, int level, int lease, long rate, boolean addNew)
	{
		if (player == null)
		{
			return false;
		}
		if ((lease > 0) && !player.destroyItemByItemId(null, Inventory.ADENA_ID, lease, null, true))
		{
			return false;
		}
		if (addNew)
		{
			_function.put(type, new FortFunction(type, level, lease, 0, rate, 0, false));
		}
		else if ((level == 0) && (lease == 0))
		{
			removeFunction(type);
		}
		else if ((lease - _function.get(type).getLease()) > 0)
		{
			_function.remove(type);
			_function.put(type, new FortFunction(type, level, lease, 0, rate, -1, false));
		}
		else
		{
			_function.get(type).setLease(lease);
			_function.get(type).setLevel(level);
			_function.get(type).dbSave();
		}
		return true;
	}
	
	public void activateInstance()
	{
		loadDoor();
	}
	
	// This method loads fort door data from database
	private void loadDoor()
	{
		for (Door door : DoorData.getInstance().getDoors())
		{
			if ((door.getFort() != null) && (door.getFort().getResidenceId() == getResidenceId()))
			{
				_doors.add(door);
			}
		}
	}
	
	private void loadFlagPoles()
	{
		for (StaticObject obj : StaticObjectData.getInstance().getStaticObjects())
		{
			if ((obj.getType() == 3) && obj.getName().startsWith(getName()))
			{
				_flagPole = obj;
				break;
			}
		}
		if (_flagPole == null)
		{
			throw new NullPointerException("Can't find flagpole for Fort " + this);
		}
	}
	
	// This method loads fort door upgrade data from database
	private void loadDoorUpgrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_doorupgrade WHERE fortId = ?"))
		{
			ps.setInt(1, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	private void removeDoorUpgrade()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM fort_doorupgrade WHERE fortId = ?"))
		{
			ps.setInt(1, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO fort_doorupgrade (doorId, hp, pDef, mDef) VALUES (?,?,?,?)"))
		{
			ps.setInt(1, doorId);
			ps.setInt(2, hp);
			ps.setInt(3, pDef);
			ps.setInt(4, mDef);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage(), e);
		}
	}
	
	private void updateOwnerInDB()
	{
		final Clan clan = _fortOwner;
		int clanId = 0;
		if (clan != null)
		{
			clanId = clan.getId();
			_lastOwnedTime.setTimeInMillis(System.currentTimeMillis());
		}
		else
		{
			_lastOwnedTime.setTimeInMillis(0);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET owner=?,lastOwnedTime=?,state=?,castleId=? WHERE id = ?"))
		{
			ps.setInt(1, clanId);
			ps.setLong(2, _lastOwnedTime.getTimeInMillis());
			ps.setInt(3, 0);
			ps.setInt(4, 0);
			ps.setInt(5, getResidenceId());
			ps.execute();
			
			// Announce to clan members
			if (clan != null)
			{
				clan.setFortId(getResidenceId()); // Set has fort flag for new owner
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.S1_IS_VICTORIOUS_IN_THE_FORTRESS_BATTLE_OF_S2);
				sm.addString(clan.getName());
				sm.addCastleId(getResidenceId());
				World.getInstance().getPlayers().forEach(p -> p.sendPacket(sm));
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				if (_fortUpdater[0] != null)
				{
					_fortUpdater[0].cancel(false);
				}
				if (_fortUpdater[1] != null)
				{
					_fortUpdater[1].cancel(false);
				}
				_fortUpdater[0] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, 0, FortUpdaterType.PERIODIC_UPDATE), Config.FS_UPDATE_FRQ * 60000, Config.FS_UPDATE_FRQ * 60000); // Schedule owner tasks to start running
				if (Config.FS_MAX_OWN_TIME > 0)
				{
					_fortUpdater[1] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, 0, FortUpdaterType.MAX_OWN_TIME), 3600000, 3600000); // Schedule owner tasks to remove owner
				}
			}
			else
			{
				if (_fortUpdater[0] != null)
				{
					_fortUpdater[0].cancel(false);
				}
				_fortUpdater[0] = null;
				if (_fortUpdater[1] != null)
				{
					_fortUpdater[1].cancel(false);
				}
				_fortUpdater[1] = null;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + e.getMessage(), e);
		}
	}
	
	@Override
	public int getOwnerId()
	{
		final Clan clan = _fortOwner;
		return clan != null ? clan.getId() : -1;
	}
	
	public Clan getOwnerClan()
	{
		return _fortOwner;
	}
	
	public void setOwnerClan(Clan clan)
	{
		setVisibleFlag(clan != null);
		_fortOwner = clan;
	}
	
	public Door getDoor(int doorId)
	{
		if (doorId <= 0)
		{
			return null;
		}
		
		for (Door door : _doors)
		{
			if (door.getId() == doorId)
			{
				return door;
			}
		}
		return null;
	}
	
	public List<Door> getDoors()
	{
		return _doors;
	}
	
	public StaticObject getFlagPole()
	{
		return _flagPole;
	}
	
	public FortSiege getSiege()
	{
		if (_siege == null)
		{
			synchronized (this)
			{
				if (_siege == null)
				{
					_siege = new FortSiege(this);
				}
			}
		}
		return _siege;
	}
	
	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	public void setSiegeDate(Calendar siegeDate)
	{
		_siegeDate = siegeDate;
	}
	
	public int getOwnedTime()
	{
		return _lastOwnedTime.getTimeInMillis() == 0 ? 0 : (int) ((System.currentTimeMillis() - _lastOwnedTime.getTimeInMillis()) / 1000);
	}
	
	public int getTimeTillRebelArmy()
	{
		return _lastOwnedTime.getTimeInMillis() == 0 ? 0 : (int) (((_lastOwnedTime.getTimeInMillis() + (Config.FS_MAX_OWN_TIME * 3600000)) - System.currentTimeMillis()) / 1000);
	}
	
	public long getTimeTillNextFortUpdate()
	{
		return _fortUpdater[0] == null ? 0 : _fortUpdater[0].getDelay(TimeUnit.SECONDS);
	}
	
	public void updateClansReputation(Clan owner, boolean removePoints)
	{
		if (owner != null)
		{
			if (removePoints)
			{
				owner.takeReputationScore(Config.LOOSE_FORT_POINTS);
			}
			else
			{
				owner.addReputationScore(Config.TAKE_FORT_POINTS);
			}
		}
	}
	
	private static class endFortressSiege implements Runnable
	{
		private final Fort _f;
		private final Clan _clan;
		
		public endFortressSiege(Fort f, Clan clan)
		{
			_f = f;
			_clan = clan;
		}
		
		@Override
		public void run()
		{
			try
			{
				_f.setOwner(_clan, true);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception in endFortressSiege " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * @return Returns state of fortress.<br>
	 *         0 - not decided yet<br>
	 *         1 - independent<br>
	 *         2 - contracted with castle
	 */
	public int getFortState()
	{
		return _state;
	}
	
	/**
	 * @param state
	 *            <ul>
	 *            <li>0 - not decided yet</li>
	 *            <li>1 - independent</li>
	 *            <li>2 - contracted with castle</li>
	 *            </ul>
	 * @param castleId the Id of the contracted castle (0 if no contract with any castle)
	 */
	public void setFortState(int state, int castleId)
	{
		_state = state;
		_castleId = castleId;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE fort SET state=?,castleId=? WHERE id = ?"))
		{
			ps.setInt(1, _state);
			ps.setInt(2, _castleId);
			ps.setInt(3, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: setFortState(int state, int castleId): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @return the fortress type (0 - small (3 commanders), 1 - big (4 commanders + control room))
	 */
	public int getFortType()
	{
		return _fortType;
	}
	
	/**
	 * @param npcId the Id of the ambassador NPC
	 * @return the Id of the castle this ambassador represents
	 */
	public int getCastleIdByAmbassador(int npcId)
	{
		if ((_envoyCastles == null) || !_envoyCastles.containsKey(npcId))
		{
			return -1;
		}
		return _envoyCastles.get(npcId);
	}
	
	/**
	 * @param npcId the Id of the ambassador NPC
	 * @return the castle this ambassador represents
	 */
	public Castle getCastleByAmbassador(int npcId)
	{
		return CastleManager.getInstance().getCastleById(getCastleIdByAmbassador(npcId));
	}
	
	/**
	 * @return the Id of the castle contracted with this fortress
	 */
	public int getContractedCastleId()
	{
		return _castleId;
	}
	
	/**
	 * @return the castle contracted with this fortress ({@code null} if no contract with any castle)
	 */
	public Castle getContractedCastle()
	{
		return CastleManager.getInstance().getCastleById(getContractedCastleId());
	}
	
	/**
	 * Check if this is a border fortress (associated with multiple castles).
	 * @return {@code true} if this is a border fortress (associated with more than one castle), {@code false} otherwise
	 */
	public boolean isBorderFortress()
	{
		return _availableCastles.size() > 1;
	}
	
	/**
	 * @return the amount of barracks in this fortress
	 */
	public int getFortSize()
	{
		return _fortType == 0 ? 3 : 5;
	}
	
	public void spawnSuspiciousMerchant()
	{
		if (_isSuspiciousMerchantSpawned)
		{
			return;
		}
		
		_isSuspiciousMerchantSpawned = true;
		for (Spawn spawnDat : _siegeNpcs)
		{
			spawnDat.doSpawn(false);
			spawnDat.startRespawn();
		}
	}
	
	public void despawnSuspiciousMerchant()
	{
		if (!_isSuspiciousMerchantSpawned)
		{
			return;
		}
		_isSuspiciousMerchantSpawned = false;
		for (Spawn spawnDat : _siegeNpcs)
		{
			spawnDat.stopRespawn();
			spawnDat.getLastSpawn().deleteMe();
		}
	}
	
	public void spawnNpcCommanders()
	{
		for (Spawn spawnDat : _npcCommanders)
		{
			spawnDat.doSpawn(false);
			spawnDat.startRespawn();
		}
	}
	
	public void despawnNpcCommanders()
	{
		for (Spawn spawnDat : _npcCommanders)
		{
			spawnDat.stopRespawn();
			spawnDat.getLastSpawn().deleteMe();
		}
	}
	
	public void spawnSpecialEnvoys()
	{
		for (Spawn spawnDat : _specialEnvoys)
		{
			spawnDat.doSpawn(false);
			spawnDat.startRespawn();
		}
	}
	
	private void initNpcs()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_spawnlist WHERE fortId = ? AND spawnType = ?"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 0);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawnDat = new Spawn(rs.getInt("npcId"));
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					SpawnTable.getInstance().addSpawn(spawnDat);
					spawnDat.doSpawn(false);
					spawnDat.startRespawn();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initNpcs: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	private void initSiegeNpcs()
	{
		_siegeNpcs.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 2);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawnDat = new Spawn(rs.getInt("npcId"));
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					_siegeNpcs.add(spawnDat);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initSiegeNpcs: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	private void initNpcCommanders()
	{
		_npcCommanders.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 1);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Spawn spawnDat = new Spawn(rs.getInt("npcId"));
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					_npcCommanders.add(spawnDat);
				}
			}
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initNpcCommanders: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	private void initSpecialEnvoys()
	{
		_specialEnvoys.clear();
		_envoyCastles.clear();
		_availableCastles.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading, castleId FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"))
		{
			ps.setInt(1, getResidenceId());
			ps.setInt(2, 3);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int castleId = rs.getInt("castleId");
					final int npcId = rs.getInt("npcId");
					final Spawn spawnDat = new Spawn(npcId);
					spawnDat.setAmount(1);
					spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
					spawnDat.setHeading(rs.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					_specialEnvoys.add(spawnDat);
					_envoyCastles.put(npcId, castleId);
					_availableCastles.add(castleId);
				}
			}
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			LOGGER.log(Level.WARNING, "Fort " + getResidenceId() + " initSpecialEnvoys: Spawn could not be initialized: " + e.getMessage(), e);
		}
	}
	
	@Override
	protected void initResidenceZone()
	{
		for (FortZone zone : ZoneManager.getInstance().getAllZones(FortZone.class))
		{
			if (zone.getResidenceId() == getResidenceId())
			{
				setResidenceZone(zone);
				break;
			}
		}
	}
}
