package Zeze.World;

import java.util.concurrent.ConcurrentHashMap;

public class MapManagerDefault implements MapManager {
	private final ConcurrentHashMap<Long, CubeMap> cubeMaps = new ConcurrentHashMap<>();

	@Override
	public void enterMap(int mapId) {
	}

	@Override
	public CubeMap getCubeMap(long instanceId) {
		return cubeMaps.get(instanceId);
	}
}
