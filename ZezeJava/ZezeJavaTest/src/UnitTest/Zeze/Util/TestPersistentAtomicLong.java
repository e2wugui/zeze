package UnitTest.Zeze.Util;
import harness.Fast;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@SuppressWarnings("CallToPrintStackTrace")
@Fast
public class TestPersistentAtomicLong {
	@Test
	public void testConcurrent() {
		Task.tryInitThreadPool();

		var p1 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var p2 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var jobs = new ArrayList<Future<?>>();
		jobs.add(TaskSpec.ofAction(() -> Alloc(p1)).name("Alloc1").submitNow());
		jobs.add(TaskSpec.ofAction(() -> Alloc(p2)).name("Alloc2").submitNow());
		Task.waitAll(jobs);
	}

	final ConcurrentHashMap<Long, Long> allocs = new ConcurrentHashMap<>();

	private void Alloc(PersistentAtomicLong p) {
		try {
			for (int i = 0; i < 1000; ++i) {
				var n = p.next();
				Assertions.assertNull(allocs.put(n, n));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Assertions.fail();
		}
	}
}
