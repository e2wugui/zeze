package UnitTest.Zeze.Util;

import Zeze.Util.FastLock;
import org.junit.Assert;
import org.junit.Test;

public class TestFastLock {
	@Test
	public void testFastLock() throws InterruptedException {
		final int THREAD_COUNT = 20;
		final int LOOP_COUNT = 10_000;

		var lock = new FastLock();
		var value = new int[1];
		var ts = new Thread[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++) {
			(ts[i] = new Thread(() -> {
				for (int j = 0; j < LOOP_COUNT; j++) {
					lock.lock();
					try {
						var v = value[0];
						Thread.yield();
						value[0] = v + 1;
					} finally {
						lock.unlock();
					}
				}
			}, "testFastLockThread-" + i)).start();
		}
		for (var t : ts)
			t.join();
		Assert.assertEquals(THREAD_COUNT * LOOP_COUNT, value[0]);
		System.out.println("testFastLock OK");
	}
}
