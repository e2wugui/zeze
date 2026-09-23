package UnitTest.Zeze.Util;

import java.util.Calendar;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Util.DaemonTimer;
import Zeze.Util.Task;
import harness.Fast;

/**
 * DaemonTimer（FND7-17/18"守护停机三件套"的组件化）行为钉板：
 * <ol>
 * <li>守护体必须跑在worker池线程而非调度线程——内联形态（缺陷）下线程名命中
 * ZezeScheduledPool即红；且多轮执行永不重叠（自续约链的结构性质）；</li>
 * <li>stop()必须限时等待在飞一轮：守护体被阻塞期间stop不得完成（缺等待形态立即完成，红），
 * 放行后stop正常返回；</li>
 * <li>关门：stop返回后不再触发新一轮（计数冻结）；restart：stop后start恢复周期执行；</li>
 * <li>stop超预算逃逸（timeoutMs小值使预算可等满）：逃逸轮不占链（stop返回后在飞标志已摘）、
 * 收尾不续约新轮，restart后新链正常。</li>
 * </ol>
 */
@Fast
public class TestDaemonTimer {

	@BeforeAll
	public static void setUp() {
		Task.tryInitThreadPool();
	}

	@AfterAll
	public static void tearDown() {
		// 池由fast套件共享，不在此关闭
	}

	private static boolean waitUntil(Check cond, long timeoutMs) throws Exception {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (cond.check())
				return true;
			//noinspection BusyWait
			Thread.sleep(10);
		}
		return cond.check();
	}

	private interface Check {
		boolean check();
	}

	@Test
	@Timeout(30)
	public void testBodyRunsOffSchedulerAndNeverOverlaps() throws Exception {
		var runs = new AtomicInteger();
		var concurrent = new AtomicInteger();
		var threadName = new AtomicReference<String>("");
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.offScheduler", 80, () -> {
			Assertions.assertEquals(1, concurrent.incrementAndGet(), "守护体不得重叠执行");
			try {
				threadName.set(Thread.currentThread().getName());
				runs.incrementAndGet();
			} finally {
				concurrent.decrementAndGet();
			}
		});
		try {
			daemon.start();
			Assertions.assertTrue(waitUntil(() -> runs.get() >= 3, 5_000), "周期执行必须在5s内到达3轮");
		} finally {
			daemon.stop();
		}
		// FND7-17钉板：内联形态（守护体直接跑在调度线程）下线程名含ScheduledPool即红
		Assertions.assertFalse(threadName.get().contains("Scheduled"),
				"守护体不得在调度线程上执行，实际线程=" + threadName.get());
		// 关门钉板：stop返回后计数冻结
		var frozen = runs.get();
		Thread.sleep(300); // 80ms周期下若未关门将多跑约3轮
		Assertions.assertEquals(frozen, runs.get(), "stop返回后不得再触发新一轮");
	}

	@Test
	@Timeout(30)
	public void testStopWaitsInFlightAndRestart() throws Exception {
		var bodyEntered = new CountDownLatch(1);
		var bodyGate = new CountDownLatch(1);
		var bodyDone = new CountDownLatch(1);
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.stopWaits", 80, () -> {
			bodyEntered.countDown();
			Assertions.assertTrue(bodyGate.await(30, TimeUnit.SECONDS), "测试必须及时放行守护体");
			bodyDone.countDown();
		});
		var stopper = Executors.newSingleThreadExecutor(r -> new Thread(r, "UnitTest.DaemonTimer.stop"));
		try {
			daemon.start();
			Assertions.assertTrue(bodyEntered.await(5, TimeUnit.SECONDS), "首轮必须在5s内触发");
			Assertions.assertTrue(daemon.isBusy(), "守护体在飞标志必须置位");

			Future<?> stopFuture = stopper.submit(() -> {
				daemon.stop();
				return null;
			});
			// FND7-18钉板：守护体被阻塞期间stop必须等待（缺等待形态立即返回，红）
			try {
				stopFuture.get(500, TimeUnit.MILLISECONDS);
				Assertions.fail("stop()必须在在飞守护体结束前等待");
			} catch (java.util.concurrent.TimeoutException expected) {
				// 期望：stop仍在等待
			}
			bodyGate.countDown();
			stopFuture.get(5, TimeUnit.SECONDS); // 放行后stop正常完成
			Assertions.assertEquals(0, bodyDone.getCount(), "stop返回前守护体必须已跑完本轮");
			Assertions.assertFalse(daemon.isBusy(), "stop返回后不得在飞");

			// restart钉板：stop后start恢复周期执行
			var runs = new AtomicInteger();
			var daemon2 = new DaemonTimer("UnitTest.DaemonTimer.restart", 80, runs::incrementAndGet);
			daemon2.start();
			daemon2.stop();
			daemon2.start();
			Assertions.assertTrue(waitUntil(() -> runs.get() >= 1, 5_000), "restart后必须恢复周期执行");
			daemon2.stop();
		} finally {
			bodyGate.countDown();
			stopper.shutdownNow();
		}
	}

	@Test
	@Timeout(30)
	public void testStopFromBodySkipsWaiting() throws Exception {
		// body内自调stop：等自己必等满~125s预算（@Timeout兜底即红），组件须识别并跳过等待
		var ref = new AtomicReference<DaemonTimer>();
		var stopElapsedMs = new AtomicLong(-1); // -1=未触发；跳过等待后耗时<1ms可能舍入为0，不能用>0判触发
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.selfStop", 80, () -> {
			var begin = System.nanoTime();
			ref.get().stop();
			stopElapsedMs.set((System.nanoTime() - begin) / 1_000_000);
		});
		ref.set(daemon);
		daemon.start();
		Assertions.assertTrue(waitUntil(() -> stopElapsedMs.get() >= 0, 5_000), "首轮必须在5s内触发自stop");
		Assertions.assertTrue(stopElapsedMs.get() < 10_000,
				"body内自调stop不得等满预算，实际ms=" + stopElapsedMs.get());
		Assertions.assertTrue(daemon.isShutdown(), "自stop后必须处于关门状态");
		Assertions.assertFalse(daemon.isBusy(), "自stop返回后不得显示在飞");
	}

	@Test
	@Timeout(30)
	public void testSetPeriodMsEffectiveOnNextRound() throws Exception {
		// 周期调整钉板：setPeriodMs后下一次续约生效（已排期pending不重排）。
		// round29假红钉因：墙钟总预算700ms（最坏路径pending300+2×50=400ms+余量300ms）在白天
		// 负载下fire/worker滞后数百ms即假红（实测799）。改body内逐轮时间戳断言并拉开周期档距
		// （旧1500/新50），逐段双向可判且各留数百ms调度抖动余量：
		//   g1=第2→3轮：旧pending保留——若setPeriodMs错误地立即重排pending，g1塌缩到~50ms档
		//   g2=第3→4轮、g3=第4→5轮：新周期必须已在第3轮收尾续约时生效——永不生效则不破1500ms档
		var times = new ConcurrentLinkedQueue<Long>();
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.setPeriod", 1500,
				() -> times.add(System.currentTimeMillis()));
		try {
			daemon.start();
			Assertions.assertTrue(waitUntil(() -> times.size() >= 2, 10_000), "1500ms周期必须在10s内到达2轮");
			daemon.setPeriodMs(50);
			Assertions.assertTrue(waitUntil(() -> times.size() >= 5, 10_000), "调小周期后必须到达5轮");
			var t = times.toArray(new Long[0]);
			var g1 = t[2] - t[1];
			var g2 = t[3] - t[2];
			var g3 = t[4] - t[3];
			Assertions.assertTrue(g1 >= 700, "已排期pending不得被立即重排，第2→3轮间隔ms=" + g1);
			Assertions.assertTrue(g2 <= 800, "新周期必须于下一次续约生效，第3→4轮间隔ms=" + g2);
			Assertions.assertTrue(g3 <= 800, "新周期必须持续生效，第4→5轮间隔ms=" + g3);
		} finally {
			daemon.stop();
		}
	}

	@Test
	@Timeout(30)
	public void testNextDelaySupplierPerRound() throws Exception {
		// 逐轮延迟钉板：供应商每次续约时求值（Token每日锚点重对齐的机制基础）。
		// 序列300,50,50,...：若首值被缓存则恒300ms节奏，逐轮求值则第3轮起50ms节奏。
		var runs = new AtomicInteger();
		var firstDelay = new AtomicBoolean(true);
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.nextDelay",
				() -> firstDelay.getAndSet(false) ? 300 : 50, 0, runs::incrementAndGet);
		try {
			daemon.start();
			Assertions.assertTrue(waitUntil(() -> runs.get() >= 2, 5_000), "首轮300ms后必须在5s内到达2轮");
			var begin = System.currentTimeMillis();
			Assertions.assertTrue(waitUntil(() -> runs.get() >= 5, 3_000), "后续轮必须在3s内到达");
			var elapsed = System.currentTimeMillis() - begin;
			// 缓存首值则3×300=900ms；逐轮求值最坏=已排期pending(50)+2×50=150ms
			Assertions.assertTrue(elapsed <= 400, "供应商延迟必须逐轮求值生效，实际ms=" + elapsed);
		} finally {
			daemon.stop();
		}

		// 违约关门钉板：供应商返回<=0即关门终止（scheduleNow的<=0会即时触发成busy环）
		var badRuns = new AtomicInteger();
		var bad = new DaemonTimer("UnitTest.DaemonTimer.nextDelayBad", () -> 0, 0, badRuns::incrementAndGet);
		bad.start();
		Assertions.assertTrue(waitUntil(bad::isShutdown, 1_000), "违约供应商必须关门");
		Thread.sleep(200);
		Assertions.assertEquals(0, badRuns.get(), "关门后不得有轮次执行");
	}

	@Test
	@Timeout(30)
	public void testSupplierExceptionShutsDownChain() throws Exception {
		// 供应商抛异常钉板：与<=0同罪必须关门——修复前异常逃出rescheduleLocked（求值在
		// try之外），链断但shutdown=false、无pending无在飞，isShutdown()报false的僵尸守护。
		var runs = new AtomicInteger();
		var firstDelay = new AtomicBoolean(true);
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.nextDelayThrow",
				() -> {
					if (firstDelay.getAndSet(false))
						return 50;
					throw new IllegalStateException("UnitTest nextDelayMs supplier boom");
				}, 0, runs::incrementAndGet);
		daemon.start();
		Assertions.assertTrue(waitUntil(() -> runs.get() >= 1, 5_000), "首轮50ms后必须执行");
		Assertions.assertTrue(waitUntil(daemon::isShutdown, 5_000),
				"供应商抛异常必须关门（修复前僵尸：isShutdown恒false且链已断）");
		Assertions.assertEquals(1, runs.get(), "违约后计数冻结在首轮");
		Thread.sleep(300);
		Assertions.assertEquals(1, runs.get(), "关门后不得再有轮次执行");
	}

	@Test
	@Timeout(30)
	public void testDelayUntilNextDailyAlwaysAtLeast1Ms() throws Exception {
		// delayUntilNextDaily钳制钉板（Token.cleanTokenMapTableDaemon的延迟来源）：
		// 求值落在锚点毫秒（或内部before()与结尾两次取时刻跨过锚点）时旧实现算出0/负数，
		// 会被DaemonTimer供应商模式定性为违约关门。锚点毫秒无法廉价确定性命中，密集采样
		// 可达锚点断言恒>=1；上限=次日同刻+1分钟（当前分钟锚点已过即排明天）。
		var cal = Calendar.getInstance();
		int curH = cal.get(Calendar.HOUR_OF_DAY);
		int curM = cal.get(Calendar.MINUTE);
		cal.add(Calendar.MINUTE, 1);
		int nextH = cal.get(Calendar.HOUR_OF_DAY);
		int nextM = cal.get(Calendar.MINUTE);
		var anchors = new int[][] {{curH, curM}, {nextH, nextM}, {3, 14}, {23, 59}};
		var deadline = System.currentTimeMillis() + 200;
		while (System.currentTimeMillis() < deadline) {
			for (var anchor : anchors) {
				var delay = Task.delayUntilNextDaily(anchor[0], anchor[1]);
				Assertions.assertTrue(delay >= 1, "delay必须>=1ms，anchor=" + anchor[0] + ":" + anchor[1]
						+ "，实际=" + delay);
				Assertions.assertTrue(delay <= 24 * 3600_000L + 60_000,
						"delay不得超过次日同刻+1分钟，实际=" + delay);
			}
		}
	}

	@Test
	@Timeout(30)
	public void testStopEscapedRoundDoesNotOccupyChain() throws Exception {
		// 逃逸路径钉板：timeoutMs=300使预算(300+5000=5300ms)可等满。逃逸轮用不可中断等待
		// （parkNanos循环）——tiny timeout下若被看门狗interrupt，latch/sleep会被打断造成假红
		var escaped = new AtomicBoolean(false); // 首轮即逃逸轮
		var release = new AtomicBoolean(false);
		var escapeDone = new AtomicBoolean(false);
		var laterRounds = new AtomicInteger();
		var daemon = new DaemonTimer("UnitTest.DaemonTimer.escape", 80, 300, () -> {
			if (!escaped.compareAndSet(false, true)) {
				laterRounds.incrementAndGet();
				return;
			}
			while (!release.get())
				LockSupport.parkNanos(50_000_000);
			escapeDone.set(true);
		});
		daemon.start();
		// round27假红钉因：isBusy()含"已派发未进入body"（fire在调度线程置inFlight后才派发worker池），
		// 该形态下stop的awaitIdle被runBody入口的迟到轮作废逻辑立即唤醒（设计行为），stopElapsed>=5000必假红。
		// 等待点绑定body自己的进入信号（escaped CAS）：逃逸轮已park在body内，stop的限时预算才有对象可等。
		Assertions.assertTrue(waitUntil(escaped::get, 5_000), "逃逸轮必须已进入body（park中）");

		var begin = System.currentTimeMillis();
		daemon.stop(); // 预算5300ms < 逃逸轮等待时长：超时告警返回
		var stopElapsed = System.currentTimeMillis() - begin;
		Assertions.assertTrue(stopElapsed >= 5_000, "stop必须等满预算才返回，实际ms=" + stopElapsed);
		Assertions.assertFalse(escapeDone.get(), "预算耗尽时逃逸轮必须仍在跑（stop先行返回）");
		Assertions.assertTrue(daemon.isShutdown(), "stop后关门");
		Assertions.assertFalse(daemon.isBusy(), "stop已摘走在飞标志（逃逸轮不占链）");

		release.set(true); // 放行逃逸轮
		Assertions.assertTrue(waitUntil(escapeDone::get, 5_000), "放行后逃逸轮必须跑完");
		Assertions.assertEquals(0, laterRounds.get(), "逃逸轮收尾不得续约新轮（已关门）");

		daemon.start(); // restart钉板：逃逸轮曾占用worker线程，新链不得被它拖累
		Assertions.assertTrue(waitUntil(() -> laterRounds.get() >= 1, 5_000), "restart后新轮必须正常触发");
		daemon.stop();
	}
}
