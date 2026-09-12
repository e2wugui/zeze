package UnitTest.Zeze;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND3-49 回归：checkpointFuture 竞态的终态症状——字段挂着"已完成的future哨兵"时，
 * checkpointRunThread不得永久跳过。
 * 竞态机制：pool.submit先入队后返回Future，任务可能在提交线程赋值字段之前跑完，
 * 任务内finally的清空发生在赋值前=白清，随后赋值把已完成的future挂进字段，
 * 之后所有调用因字段非null跳过，手动checkpoint失效直到stop()。
 * 测试直接注入哨兵终态验证症状，不依赖低概率真竞态（复核实证"稳定命中"不成立）。
 * 修复：判据化——"在跑中"=字段非null且未完成，任务不再触碰字段。
 */
@Fast
public class TestCheckpointRunThreadSentinel {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache与Memory库互撞（对齐TestAutoKeyInvalidateRange）。
	// 750段：避开200/400/500/700/730已占用段。
	private static final int ServerId = 750;

	private Application app;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("checkpoint_sentinel_test_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestCheckpointRunThreadSentinel", conf);
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	@Test
	public void testCompletedFutureNotBlockNextRun() throws Exception {
		var field = Application.class.getDeclaredField("checkpointFuture");
		field.setAccessible(true);
		var stale = CompletableFuture.completedFuture(null);
		field.set(app, stale); // 注入竞态终态：已完成的future被迟到赋值挂进字段

		app.checkpointRunThread();

		var current = (Future<?>)field.get(app);
		assertNotNull(current, "应提交新任务");
		assertNotSame(stale, current, "已完成哨兵不得阻断新任务提交（FND3-49）");
		current.get(5, TimeUnit.SECONDS); // 新任务正常完成（Table模式空数据runOnce安全）
	}

	/**
	 * 并发压测准入不变量："字段被替换时，被替换的旧future必已完成"（至多一个checkpoint在跑）。
	 * 检查零误报：future完成单调——检查时未完成⇒替换时也未完成⇒必是"上一个没跑完就提交新的"双跑违反。
	 */
	@Test
	public void testConcurrentHammerNoOverrun() throws Exception {
		var field = Application.class.getDeclaredField("checkpointFuture");
		field.setAccessible(true);
		var violation = new AtomicReference<String>();
		Runnable hammer = () -> {
			try {
				for (var i = 0; i < 2000; i++) {
					var before = (Future<?>)field.get(app); // 无锁采样：竞态采样正是要的
					app.checkpointRunThread();
					var after = (Future<?>)field.get(app);
					if (before != null && before != after && !before.isDone())
						violation.set("replaced running future: " + before);
				}
			} catch (IllegalAccessException e) {
				violation.set("reflect: " + e);
			}
		};
		var threads = new ArrayList<Thread>();
		for (var t = 0; t < 4; t++)
			threads.add(Thread.ofPlatform().daemon().start(hammer));
		for (var th : threads)
			th.join(30_000);
		assertNull(violation.get());
	}
}
