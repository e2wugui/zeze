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

	@Test
	public void testNextCountNotExceedAllocatedEnd() throws Exception {
		Task.tryInitThreadPool();

		var pal = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong.U3");
		// 反射构造"剩余预算不足count"的状态：currentId=100, allocatedEnd=102（剩余2），请求next(5)。
		var fCurrent = PersistentAtomicLong.class.getDeclaredField("currentId");
		fCurrent.setAccessible(true);
		((java.util.concurrent.atomic.AtomicLong)fCurrent.get(pal)).set(100);
		var fEnd = PersistentAtomicLong.class.getDeclaredField("allocatedEnd");
		fEnd.setAccessible(true);
		fEnd.setLong(pal, 102);

		var returned = pal.next(5);

		// 语义：count个号的整块[current+1, current+count]必须落在水位allocatedEnd之内。
		// 越过水位的号在重启后被重复发放（重启时currentId重置为文件水位），
		// 多进程共享同一pal文件时直接与其他进程的区间冲突。
		var endAfter = fEnd.getLong(pal);
		Assertions.assertEquals(105, returned);
		Assertions.assertTrue(returned <= endAfter,
				"next(count)不得越过allocatedEnd发号: returned=" + returned + ", allocatedEnd=" + endAfter);
		Assertions.assertEquals(105, ((java.util.concurrent.atomic.AtomicLong)fCurrent.get(pal)).get());
	}
}
