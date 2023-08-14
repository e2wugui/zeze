package Zeze.Hot;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

public class HotGuard implements Closeable {
	private final Lock lock;

	public HotGuard(Lock lock) {
		//System.out.println("enter lock " + lock);
		lock.lock();
		//System.out.println("enter lock OK " + lock);
		//new Exception().printStackTrace();
		this.lock = lock;
	}

	@Override
	public void close() throws IOException {
		//System.out.println("exit lock " + lock);
		lock.unlock();
		//System.out.println("exit lock OK " + lock);
	}
}
