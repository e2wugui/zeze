package UnitTest.Zeze.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BNode;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Component.AbstractTimer;
import Zeze.Component.Timer;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerSpec;
import Zeze.Config;
import Zeze.Transaction.TableX;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CP2-F1 回归（P1）：loadTimer装载期发现RunOnce策略定时器迟到（nextExpectedTime在过去）
 * 时补触发——原先包在Transaction.whileCommit内同步执行dispatchFire：commit回调运行在
 * 提交线程、事务已Completed，空oneByOneKey（绝大多数调用）时fireSimple内首个bean写
 * （beforeCallSimpleTimer的setExpectedTime→putLog）必抛IllegalStateException，被吞成
 * 日志后补触发丢失，且continue跳过了常规调度——该定时器永久停摆，每次重启重复失败。
 * 修复：改用事务感知的TaskSpec.ofAction(...).run()（提交后入池执行），fireSimple跑在
 * 无事务的池线程上，newProcedure新建事务。
 * 测试：调度RunOnce定时器（oneByOneKey为默认空串）→backdate到过去→重启Timer触发
 * loadTimer→断言用户回调真正执行（修复前：回调一次都不执行，定时器永久停摆）。
 */
@Fast
public class TestTimerLoadMissfireAsync {

	// 独立serverId+url：@Fast类并行时避免本地RocksCache与Memory库互撞（对齐TakeoverTestEnv）。
	// 760段：避开已占用的200/400/500/700/730/750段。
	private static final int ServerId = 760;

	private static final long Period = 60_000;
	private static final CountDownLatch FiredLatch = new CountDownLatch(1);
	private static final AtomicInteger FireCount = new AtomicInteger();

	private Application app;
	private Timer timer;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("timer_missfire_load_test_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		conf.setTakeoverMode("off");
		app = new Application("TestTimerLoadMissfireAsync", conf);
		// Timer模块必须在start之前注册（demo.App同序）；AccessibleTimer暴露protected表访问。
		timer = new TakeoverTestEnv.AccessibleTimer(new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		});
		app.start();
		timer.loadCustomClassAnd(); // 初始化nodeId/timerId/timerSerial AutoKey（demo.App同型调用）
	}

	@AfterEach
	public void tearDown() throws Exception {
		timer.stop();
		app.stop();
	}

	/** 用户回调句柄：记录触发次数（类由Timer按名字反射实例化，须有默认构造）。 */
	public static class CountingHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			FireCount.incrementAndGet();
			FiredLatch.countDown();
		}
	}

	@Test
	public void testRunOnceMissfireFiresOnPoolAfterReload() throws Exception {
		timer.start();
		// 调度RunOnce策略定时器：首次触发推迟到远未来，oneByOneKey保持默认空串
		// （暴露"空key在commit回调内同步直跑"的失效形态）
		var timerIdHolder = new String[1];
		var rc = app.newProcedure(() -> {
			timerIdHolder[0] = timer.schedule(
					TimerSpec.ofDelay(3_600_000L).period(Period).times(-1)
							.missfirePolicy(AbstractTimer.eMissfirePolicyRunOnce),
					CountingHandle.class, null);
			return Procedure.Success;
		}, "TestTimerLoadMissfireAsync.schedule").call();
		assertEquals(Procedure.Success, rc);
		var timerId = timerIdHolder[0];
		assertTrue(hasFuture(timerId), "常规调度必须已安装future");

		// 模拟停机迟到：backdate到60秒前
		rc = app.newProcedure(() -> {
			var index = tIndexs().get(timerId);
			assertTrue(index != null, "index行必须存在");
			var node = tNodes().get(index.getNodeId());
			assertTrue(node != null, "node行必须存在");
			var simpleTimer = (BSimpleTimer)node.getTimers().get(timerId).getTimerObj().getBean();
			simpleTimer.setNextExpectedTime(System.currentTimeMillis() - 60_000);
			return Procedure.Success;
		}, "TestTimerLoadMissfireAsync.backdate").call();
		assertEquals(Procedure.Success, rc);

		// 重启Timer：start→loadTimer发现missfire（RunOnce分支）
		timer.stop();
		timer.start();

		// 核心（红断言）：missfire补触发必须真正执行用户回调。
		// 修复前：whileCommit回调内同步fireSimple，首个bean写抛ISE被吞，回调永不执行。
		assertTrue(FiredLatch.await(30, TimeUnit.SECONDS),
				"missfire补触发必须执行用户回调（修复前在commit回调内同步执行，首个bean写抛ISE，永久停摆）");
		assertEquals(1, FireCount.get(), "本轮只补触发一次");

		// 补触发后定时器必须恢复常规调度（不再永久停摆）。回调在fireSimple过程内先于其
		// 提交（whileCommit安装future）执行，latch醒来的瞬间future可能尚未安装，轮询等待。
		long deadline = System.currentTimeMillis() + 10_000;
		while (!hasFuture(timerId)) {
			assertTrue(System.currentTimeMillis() < deadline, "missfire后常规调度必须重建");
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	// timerFutures是Timer的包私有实例字段，测试包不同，反射读取。
	@SuppressWarnings("unchecked")
	private boolean hasFuture(String timerId) throws Exception {
		Field field = Timer.class.getDeclaredField("timerFutures");
		field.setAccessible(true);
		var futures = (ConcurrentHashMap<String, ?>)field.get(timer);
		return futures.containsKey(timerId);
	}

	@SuppressWarnings("unchecked")
	private TableX<String, BIndex> tIndexs() {
		var t = app.getTable("Zeze_Builtin_Timer_tIndexs");
		assertTrue(t != null);
		return (TableX<String, BIndex>)t;
	}

	@SuppressWarnings("unchecked")
	private TableX<Long, BNode> tNodes() {
		var t = app.getTable("Zeze_Builtin_Timer_tNodes");
		assertTrue(t != null);
		return (TableX<Long, BNode>)t;
	}
}
