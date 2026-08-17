package UnitTest.Zeze.Util;

import junit.framework.TestCase;
import org.junit.Assert;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@SuppressWarnings("CallToPrintStackTrace")
public class TestPersistentAtomicLong extends TestCase {
	public void testConcurrent() {
		Task.tryInitThreadPool();

		var p1 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var p2 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var jobs = new ArrayList<Future<?>>();
		jobs.add(TaskSpec.ofAction(() -> Alloc(p1)).name("Alloc1").runUnsafe());
		jobs.add(TaskSpec.ofAction(() -> Alloc(p2)).name("Alloc2").runUnsafe());
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
