package Zeze.World.Aoi;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BEnterConfirm;
import Zeze.Builtin.World.BSwitchWorld;
import Zeze.Builtin.World.Command;
import Zeze.Component.AutoKey;
import Zeze.Util.ConcurrentHashSet;
import Zeze.World.CubeMap;
import Zeze.World.ICommand;
import Zeze.World.IMapManager;
import Zeze.World.World;

public class MapManager implements IMapManager, ICommand {
	private final AutoKey autoKeyInstanceId;
	private final ConcurrentHashMap<Long, CubeMap> cubeMaps = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ConcurrentHashSet<Long>> maps = new ConcurrentHashMap<>();

	public final World world;

	public MapManager(World world) {
		this.autoKeyInstanceId = world.providerApp.zeze.getAutoKey("Zeze.World.AutoKeyInstanceId");
		this.world = world;

		world.internalRegisterCommand(BCommand.eSwitchWorld, this);
		world.internalRegisterCommand(BCommand.eEnterConfirm, this);
	}

	@Override
	public long handle(String playerId, Command c) throws Exception {
		switch (c.Argument.getCommandId()) {
		case BCommand.eSwitchWorld:
			return onSwitchWorld(playerId, ICommand.decode(new BSwitchWorld.Data(), c));

		case BCommand.eEnterConfirm:
			return onEnterConfirm(playerId, ICommand.decode(new BEnterConfirm.Data(), c));

		}
		return 0;
	}

	private long onSwitchWorld(String playerId, BSwitchWorld.Data arg) {
		return enterMap(arg.getMapId());
	}

	private long onEnterConfirm(String playerId, BEnterConfirm.Data arg) {
		return 0;
	}

	public CubeMap createCubeMap(long instanceId) {
		var map = new CubeMap(64, 64);
		map.setAoi(new AoiSimple(world, map, 1, 1));
		return map;
	}

	// leave after bind
	// current map instance. local leave aoi-notify.... leaveWorld(). enterMap(newMapId)
	@Override
	public long enterMap(int mapId) {
		var set = maps.computeIfAbsent(mapId, (key) -> new ConcurrentHashSet<>());
		// todo
		//  step 1 check local load, try create local;
		//  step 2 check global load, try select server and redirect;
		//  step 3 fail
		long instanceId;
		if (set.isEmpty()) {
			instanceId = autoKeyInstanceId.nextId();
			set.add(instanceId);
			cubeMaps.computeIfAbsent(instanceId, this::createCubeMap);
		} else {
			instanceId = set.iterator().next();
		}

		// bind
		// send enter to client
		// client loading ...
		// client send confirmEnter
		// server public player
		return instanceId;
	}

	@Override
	public CubeMap getCubeMap(long instanceId) {
		return cubeMaps.get(instanceId);
	}
}
