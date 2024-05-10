package UnitTest.Zeze.Trans;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Transaction.Transaction;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Task;
import demo.App;
import demo.Module1.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestLostRedo {
	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@After
	public void after() throws Exception {
		//App.Instance.Stop();
	}

	final ConcurrentHashSet<Long> keys = new ConcurrentHashSet<>();

	@Test
	public void test() throws ExecutionException, InterruptedException {
		App.Instance.Zeze.newProcedure(TestLostRedo::clear, "clear").call();
		var futures = new ArrayList<Future<?>>();
		for (int i = 0; i < 1_0000; ++i)
			futures.add(Task.runUnsafe(App.Instance.Zeze.newProcedure(this::write, "write")));
		for (var future : futures)
			future.get();
		for (var key : keys)
			App.Instance.Zeze.newProcedure(() -> verify(key), "verify").call();
	}

	private static long clear() {
		return 0; // 使用了内存表了。
	}

	private static long verify(long key) {
		var v1 = App.Instance.demo_Module1.getTable1().get(key);
		if (null != v1) {
			for (var lkey : v1.getLongList())
				Assert.assertNotEquals(null, App.Instance.demo_Module1.getTable3().get(lkey));
		}
		return 0;
	}

	private long write() {
		@SuppressWarnings("deprecation")
		var key = App.Instance.Zeze.getAutoKeyOld("lostredo.autokey").nextId();
		var mkey = key % 1000;
		keys.add(mkey);
		App.Instance.demo_Module1.getTable1().getOrAdd(mkey).getLongList().add(key);
		App.Instance.demo_Module1.getTable3().insert(key, new BValue());
		return 0;
	}

	@Test
	public void testAutoKeyConflict() throws ExecutionException, InterruptedException {
		runTimes.set(0);
		var futures = new ArrayList<Future<?>>();
		for (int i = 0; i < 1_0000; ++i)
			futures.add(Task.runUnsafe(App.Instance.Zeze.newProcedure(this::autoKeyConflict, "write")));
		for (var future : futures)
			future.get();
		System.out.println("runTimes=" + runTimes.get());
	}

	private final ConcurrentHashMap<Long, Long> autos = new ConcurrentHashMap<>();
	private final AtomicLong runTimes = new AtomicLong();
	private long autoKeyConflict() {
		runTimes.incrementAndGet();
		@SuppressWarnings("deprecation")
		var key = App.Instance.Zeze.getAutoKeyOld("conflict.autokey").nextId();
		Transaction.whileCommit(() -> Assert.assertNull(autos.putIfAbsent(key, key)));
		return 0;
	}

	@Test
	public void teatAutoKeyWithInsert() throws Exception {
		runTimes.set(0);

		var keys = new ArrayList<Long>();
		App.Instance.Zeze.checkpointRun();
		App.Instance.demo_Module1.getTable1().walk((key, value) -> keys.add(key));
		App.Instance.Zeze.newProcedure(() -> {
			for (var key : keys)
				App.Instance.demo_Module1.getTable1().remove(key);
			return 0;
		}, "clear").call();

		/*
		App.Instance.Zeze.newProcedure(() -> {
					App.Instance.Zeze.getAutoKeyOld("insert.autokey").setSeed(System.currentTimeMillis());
					return 0;
				}, "set seed to now").call();
		*/

		var count = 1000;
		var futures = new ArrayList<Future<?>>();
		for (int i = 0; i < count; ++i)
			futures.add(Task.runUnsafe(App.Instance.Zeze.newProcedure(this::autoKeyWithInsert, "write")));
		for (var future : futures)
			future.get();

		Assert.assertEquals(insertOks.get(), count);
		System.out.println("insert funTimes=" + runTimes.get());
	}

	private final AtomicLong insertOks = new AtomicLong();
	private long autoKeyWithInsert() {
		runTimes.incrementAndGet();
		@SuppressWarnings("deprecation")
		var key = App.Instance.Zeze.getAutoKeyOld("insert.autokey").nextId();
		App.Instance.demo_Module1.getTable1().insert(key, new BValue());
		Transaction.whileCommit(insertOks::incrementAndGet);
		return 0;
	}
}
