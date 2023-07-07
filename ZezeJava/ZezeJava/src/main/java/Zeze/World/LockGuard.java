package Zeze.World;

import java.io.Closeable;
import java.io.IOException;
import java.util.SortedMap;

/**
 * cubes lock helper class。
 */
public class LockGuard implements Closeable {
	private final SortedMap<CubeIndex, Cube> cubes;
	private final Cube cube;

	public LockGuard(SortedMap<CubeIndex, Cube> cubes) {
		for (var cube : cubes.values())
			cube.lock();
		this.cubes = cubes;
		this.cube = null;
	}

	public LockGuard(Cube cube) {
		cube.lock();
		this.cubes = null;
		this.cube = cube;
	}

	@Override
	public void close() throws IOException {
		if (null != cubes) {
			for (var cube : cubes.values())
				cube.unlock();
		}
		if (null != cube)
			cube.unlock();
	}
}
