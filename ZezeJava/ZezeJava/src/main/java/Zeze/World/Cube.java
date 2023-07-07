package Zeze.World;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.BObjectId;

public class Cube {
	public final CubeIndex index;
	public final HashMap<BObjectId, BObject> objects = new HashMap<>();
	public final HashMap<BObjectId, BObject> pending = new HashMap<>();

	private final ReentrantLock lock = new ReentrantLock();

	public final void lock() {
		lock.lock();
	}

	public final void unlock() {
		lock.unlock();
	}

	public Cube(CubeIndex index) {
		this.index = index;
	}
}
