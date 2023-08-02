package Zeze.Hot;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

public class HotGuard implements Closeable {
	private final Lock lock;

	public HotGuard(Lock lock) {
		lock.lock();
		this.lock = lock;
	}

	@Override
	public void close() throws IOException {
		lock.unlock();
	}
}
