package Zeze.Trans;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Transaction.Transaction;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.TaskSpec;
import demo.App;
import demo.Module1.BValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
public class TestLostRedo {
	@BeforeEach
	public void before() throws Exception {
		App.Instance.Start();
	}

	@AfterEach
	public void after() throws Exception {
		//App.Instance.Stop();
	}

	final ConcurrentHashSet<Long> keys = new ConcurrentHashSet<>();

	@Test
	public void test() throws ExecutionException, InterruptedException {
		requireNonNull(App.Instance.Zeze).newProcedure(TestLostRedo::clear, "clear").call();
		var futures = new ArrayList<Future<?>>();
		for (int i = 0; i < 1_0000; ++i) {
			futures.add(TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(this::write, "write")).submitNow());
			if ((i+1) % 200 == 0) {
				for (var future : futures)
					future.get();
				futures.clear();
			}
		}
		for (var future : futures)
			future.get();
		for (var key : keys)
			App.Instance.Zeze.newProcedure(() -> verify(key), "verify").call();
	}

	private static long clear() {
		return 0; // 使用了内存表了。
	}

	private static long verify(long key) {
		var v1 = requireNonNull(App.Instance.demo_Module1).getTable1().get(key);
		if (null != v1) {
			for (var lkey : v1.getLongList())
				Assertions.assertNotEquals(null, App.Instance.demo_Module1.getTable3().get(lkey));
		}
		return 0;
	}

	private long write() {
		var key = requireNonNull(App.Instance.Zeze).getAutoKey("lostredo.autokey").nextId();
		var mkey = key % 1000;
		keys.add(mkey);
		requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(mkey).getLongList().add(key);
		App.Instance.demo_Module1.getTable3().insert(key, new BValue());
		return 0;
	}

	@Test
	public void testAutoKeyConflict() throws ExecutionException, InterruptedException {
		runTimes.set(0);
		var futures = new ArrayList<Future<?>>();
		for (int i = 0; i < 1_0000; ++i) {
			futures.add(TaskSpec.ofProcedure(requireNonNull(App.Instance.Zeze).newProcedure(this::autoKeyConflict, "write")).submitNow());
			if ((i+1) % 200 == 0) {
				for (var future : futures)
					future.get();
				futures.clear();
			}
		}
		for (var future : futures)
			future.get();
		System.out.println("runTimes=" + runTimes.get());
	}

	private final ConcurrentHashMap<Long, Long> autos = new ConcurrentHashMap<>();
	private final AtomicLong runTimes = new AtomicLong();

	private long autoKeyConflict() {
		runTimes.incrementAndGet();
		var key = requireNonNull(App.Instance.Zeze).getAutoKey("conflict.autokey").nextId();
		Transaction.whileCommit(() -> Assertions.assertNull(autos.putIfAbsent(key, key)));
		return 0;
	}

	@Test
	public void teatAutoKeyWithInsert() throws Exception {
		runTimes.set(0);

		var keys = new ArrayList<Long>();
		requireNonNull(App.Instance.Zeze).checkpointRun();
		requireNonNull(App.Instance.demo_Module1).getTable1().walk((key, value) -> keys.add(key));
		App.Instance.Zeze.newProcedure(() -> {
			for (var key : keys)
				requireNonNull(App.Instance.demo_Module1).getTable1().remove(key);
			return 0;
		}, "clear").call();

		var count = 1000;
		var futures = new ArrayList<Future<?>>();
		for (int i = 0; i < count; ++i) {
			futures.add(TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(this::autoKeyWithInsert, "write")).submitNow());
			if ((i+1) % 200 == 0) {
				for (var future : futures)
					future.get();
				futures.clear();
			}
		}
		for (var future : futures)
			future.get();

		Assertions.assertEquals(count, insertOks.get());
		System.out.println("insert funTimes=" + runTimes.get());
	}

	private final AtomicLong insertOks = new AtomicLong();

	private long autoKeyWithInsert() {
		runTimes.incrementAndGet();
		var key = requireNonNull(App.Instance.Zeze).getAutoKey("insert.autokey").nextId();
		requireNonNull(App.Instance.demo_Module1).getTable1().insert(key, new BValue());
		Transaction.whileCommit(insertOks::incrementAndGet);
		return 0;
	}
}
