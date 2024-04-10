package Zeze.Util;

import java.util.concurrent.locks.ReentrantLock;

public class ReplayAttackMax implements ReplayAttack {
	private long max;

	private final ReentrantLock thisLock = new ReentrantLock();

	@Override
	public void lock() {
		thisLock.lock();
	}

	@Override
	public void unlock() {
		thisLock.unlock();
	}

	@Override
	public boolean replay(long serialId) {
		if (serialId > max) {
			max = serialId;
			return false;
		}
		return true;
	}
}
