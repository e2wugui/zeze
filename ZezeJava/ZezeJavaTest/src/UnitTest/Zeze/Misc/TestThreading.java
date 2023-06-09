package UnitTest.Zeze.Misc;

import java.util.HashMap;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestThreading {

	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void TestComputeIfPresent() {
		var map = new HashMap<Integer, Integer>();
		map.put(1, 1);
		map.computeIfPresent(1, (key, This) -> null);
		Assert.assertNull(map.get(1));
	}

	@Test
	public void testMutex() throws InterruptedException {
		var mutex = App.Instance.Zeze.getServiceManager().getThreading().openMutex("UnitTest.Threading.Mutex1");
		if (mutex.tryLock(1000)) {
			new Thread(() -> {
				if (mutex.tryLock(1000))
					mutex.unlock();
			}).start();
			try {
				Thread.sleep(100);
			} finally {
				mutex.unlock();
			}
		}
	}

	@Test
	public void testSemaphore() throws InterruptedException {
		var s = App.Instance.Zeze.getServiceManager().getThreading().createSemaphore("UnitTest.Threading.Semaphore", 10);
		if (s.tryAcquire(5, 1000)) {
			new Thread(() -> {
				if (s.tryAcquire(5,1000))
					s.release(5);
			}).start();
			try {
				Thread.sleep(100);
			} finally {
				s.release(5);
			}
		}
	}
}
