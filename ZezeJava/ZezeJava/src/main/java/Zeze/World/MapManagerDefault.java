package Zeze.World;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Component.AutoKey;
import Zeze.Util.ConcurrentHashSet;
import org.jetbrains.annotations.Nullable;

public class MapManagerDefault implements MapManager {
	private final AutoKey autoKeyInstanceId;
	private final ConcurrentHashMap<Long, CubeMap> cubeMaps = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ConcurrentHashSet<Long>> maps = new ConcurrentHashMap<>();

	public final World world;

	public MapManagerDefault(World world) {
		this.autoKeyInstanceId = world.providerApp.zeze.getAutoKey("Zeze.World.AutoKeyInstanceId");
		this.world = world;
	}

	@Override
	public void enterMap(int mapId) {
		var set = maps.computeIfAbsent(mapId, (key) -> new ConcurrentHashSet<>());
		// todo
		//  step 1 check local load, try create local;
		//  step 2 check global load, try select server and redirect;
		//  step 3 fail
		long instanceId;
		if (set.isEmpty()) {
			instanceId = autoKeyInstanceId.nextId();
			set.add(instanceId);
			cubeMaps.computeIfAbsent(instanceId, (key) -> new CubeMap(64, 64));
		} else {
			instanceId = set.iterator().next();
		}

		// bind
		// send enter to client
		// client loading ...
		// client send confirmEnter
		// server public player
	}

	@Override
	public CubeMap getCubeMap(long instanceId) {
		return cubeMaps.get(instanceId);
	}
}
