package Zeze.World;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Cube {
	public final CubeIndex index;
	public final HashMap<Long, Entity> objects = new HashMap<>();
	public final HashMap<Long, Entity> pending = new HashMap<>();
	public final CubeMap map;

	private final ReentrantLock lock = new ReentrantLock();

	public final void lock() {
		lock.lock();
	}

	public final void unlock() {
		lock.unlock();
	}

	public Cube(CubeIndex index, CubeMap map) {
		this.index = index;
		this.map = map;
	}
}
