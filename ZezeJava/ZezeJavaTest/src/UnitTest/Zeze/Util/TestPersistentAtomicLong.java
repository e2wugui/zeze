package UnitTest.Zeze.Util;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class TestPersistentAtomicLong extends TestCase {
	@Test
	public void testConcurrent() {
		Task.tryInitThreadPool(null, null, null);

		var p1 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong", 10);
		var p2 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong", 10);
		var jobs = new ArrayList<Future<?>>();
		jobs.add(Task.run(() -> Alloc(p1), "Alloc1"));
		jobs.add(Task.run(() -> Alloc(p2), "Alloc2"));
		Task.waitAll(jobs);
	}

	ConcurrentHashMap<Long, Long> allocs = new ConcurrentHashMap<>();

	private void Alloc(PersistentAtomicLong p) {
		try {
			for (int i = 0; i < 1000; ++i) {
				var n = p.next();
				Assert.assertEquals(allocs.put(n, n), null);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			assert false;
		}
	}
}
