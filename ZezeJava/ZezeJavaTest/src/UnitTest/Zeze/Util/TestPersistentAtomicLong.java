package UnitTest.Zeze.Util;

import Zeze.Transaction.DispatchMode;
import junit.framework.TestCase;
import org.junit.Assert;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class TestPersistentAtomicLong extends TestCase {
	public void testConcurrent() {
		Task.tryInitThreadPool();

		var p1 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var p2 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var jobs = new ArrayList<Future<?>>();
		jobs.add(Task.runUnsafe(() -> Alloc(p1), "Alloc1", DispatchMode.Normal));
		jobs.add(Task.runUnsafe(() -> Alloc(p2), "Alloc2", DispatchMode.Normal));
		Task.waitAll(jobs);
	}

	final ConcurrentHashMap<Long, Long> allocs = new ConcurrentHashMap<>();

	private void Alloc(PersistentAtomicLong p) {
		try {
			for (int i = 0; i < 1000; ++i) {
				var n = p.next();
				Assert.assertNull(allocs.put(n, n));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail();
		}
	}
}
