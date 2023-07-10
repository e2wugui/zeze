package Zeze.World;

import java.io.Closeable;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * cubes lock helper class。
 */
public class LockGuard implements Closeable {
	private SortedMap<CubeIndex, Cube> cubes;
	private Cube cube;

	private void lock(SortedMap<CubeIndex, Cube> cubes) {
		for (var cube : cubes.values())
			cube.lock();
		this.cubes = cubes;
	}

	public LockGuard(SortedMap<CubeIndex, Cube> cubes) {
		lock(cubes);
	}

	public LockGuard(Cube cube) {
		cube.lock();
		this.cube = cube;
	}

	public LockGuard(Cube cube1, Cube cube2) {
		var cubes = new TreeMap<CubeIndex, Cube>();
		cubes.put(cube1.index, cube1);
		cubes.put(cube2.index, cube2);
		lock(cubes);
	}

	public LockGuard(Cube cube1, Cube cube2, Cube cube3) {
		var cubes = new TreeMap<CubeIndex, Cube>();
		cubes.put(cube1.index, cube1);
		cubes.put(cube2.index, cube2);
		cubes.put(cube3.index, cube3);
		lock(cubes);
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
