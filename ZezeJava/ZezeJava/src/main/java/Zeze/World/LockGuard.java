package Zeze.World;

import java.io.Closeable;
import java.io.IOException;
import java.util.SortedMap;

/**
 * cubes lock helper class。
 */
public class LockGuard implements Closeable {
	private final SortedMap<CubeIndex, Cube> cubes;
	public LockGuard(SortedMap<CubeIndex, Cube> cubes) {
		for (var cube : cubes.values())
			cube.lock();
		this.cubes = cubes;
	}

	@Override
	public void close() throws IOException {
		for (var cube : cubes.values())
			cube.unlock();
	}
}
