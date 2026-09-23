package UnitTest.Zeze.Util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimerFuture;
import harness.Fast;

/**
 * TimerFuture.cancel 挂死告警钉板（无声挂死→有声，契约不变）：
 * <ol>
 * <li>cancel等锁超过阈值（timeout+CANCEL_HANG_MARGIN_MS）必须发出告警，且任务名、持锁的
 * 任务体线程名、取消线程名三者俱在（双栈在场的证据）；告警后契约不变——body未结束前
 * cancel不返回，放行后join完成、不再触发新一轮；</li>
 * <li>锁空闲时cancel即时完成且不告警。</li>
 * </ol>
 */
@Fast
public class TestTimerFutureCancelHangWarn {

	@BeforeAll
	public static void setUp() {
		Task.tryInitThreadPool();
	}

	// 独占捕获"Zeze.Util.TimerFuture"logger的WARN事件；additive=false，不影响其他logger输出
	private static final class Capture extends AbstractAppender {
		final Queue<String> messages = new ConcurrentLinkedQueue<>();

		Capture() {
			super("TestTimerFutureCancelHangWarn", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			messages.add(event.getMessage().getFormattedMessage());
		}
	}

	private LoggerContext ctx;
	private Capture capture;

	@BeforeEach
	public void attachCapture() {
		ctx = (LoggerContext)LogManager.getContext(false);
		capture = new Capture();
		capture.start();
		var lc = new LoggerConfig(TimerFuture.class.getName(), Level.WARN, false);
		lc.addAppender(capture, Level.WARN, null);
		ctx.getConfiguration().addLogger(TimerFuture.class.getName(), lc);
		ctx.updateLoggers();
	}

	@AfterEach
	public void detachCapture() {
		ctx.getConfiguration().removeLogger(TimerFuture.class.getName());
		ctx.updateLoggers();
		capture.stop();
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
	public void testCancelBlockedWarnsDualStacksThenStillJoins() throws Exception {
		var oldMargin = TimerFuture.CANCEL_HANG_MARGIN_MS;
		var lock = new ReentrantLock();
		var bodyEntered = new CountDownLatch(1);
		var rounds = new AtomicInteger();
		var bodyThreadName = new AtomicReference<String>("");
		Thread canceller = null;
		TimerFuture.CANCEL_HANG_MARGIN_MS = 200; // 阈值=timeout(300)+margin(200)=500ms
		try {
			lock.lock(); // main先持锁：body到达lock()即阻塞（此时body正持有TimerFuture锁=ABBA形状）
			var f = TaskSpec.ofAction(() -> {
				rounds.incrementAndGet();
				bodyThreadName.set(Thread.currentThread().getName());
				bodyEntered.countDown();
				lock.lock();
				lock.unlock();
			}).name("UnitTest.TimerFuture.cancelHangWarn").timeout(300).schedulePeriodNow(50, 60_000);
			Assertions.assertTrue(bodyEntered.await(5, TimeUnit.SECONDS), "首轮必须在5s内触发");

			canceller = new Thread(() -> f.cancel(false), "UnitTest.TimerFuture.canceller");
			canceller.setDaemon(true); // 兜底：断言失败时不得挂住测试JVM
			canceller.start();
			// 钉板1：等锁超阈值必须告警，任务名+双线程名俱在（双栈在场的证据）
			Assertions.assertTrue(waitUntil(() -> capture.messages.stream()
							.anyMatch(m -> m.contains("cancelHangWarn")
									&& m.contains(bodyThreadName.get())
									&& m.contains("UnitTest.TimerFuture.canceller")), 5_000),
					"cancel阻塞超阈值必须发出双栈挂死告警，实际=" + capture.messages);
			// 钉板2：契约不变——告警后body未结束前cancel不得返回
			Assertions.assertTrue(canceller.isAlive(), "body未结束前cancel不得返回");

			lock.unlock(); // 放行body→本轮结束→TimerFuture锁释放→cancel完成join
			canceller.join(5_000);
			Assertions.assertFalse(canceller.isAlive(), "放行后cancel必须完成join");
			Assertions.assertTrue(f.isCancelled());
		} finally {
			if (lock.isHeldByCurrentThread())
				lock.unlock(); // 断言失败路径也放行body，防调度线程被永久占用
			if (canceller != null)
				canceller.join(5_000);
			TimerFuture.CANCEL_HANG_MARGIN_MS = oldMargin;
		}
		// 关门钉板：cancel返回后不再触发新一轮（period=60s防自发干扰）
		var frozen = rounds.get();
		Assertions.assertEquals(1, frozen);
		Thread.sleep(300);
		Assertions.assertEquals(frozen, rounds.get(), "cancel返回后不得再触发新一轮");
	}

	@Test
	@Timeout(30)
	public void testCancelFastPathNoWarn() throws Exception {
		var rounds = new AtomicInteger();
		var firstDone = new CountDownLatch(1);
		var f = TaskSpec.ofAction(() -> {
			rounds.incrementAndGet();
			firstDone.countDown();
		}).name("UnitTest.TimerFuture.fastNoWarn").timeout(60_000).schedulePeriodNow(50, 60_000);
		Assertions.assertTrue(firstDone.await(5, TimeUnit.SECONDS), "首轮必须在5s内触发");
		f.cancel(false); // 锁空闲：tryLock立即成功，不等待也不告警
		Assertions.assertTrue(f.isCancelled());
		var frozen = rounds.get();
		Thread.sleep(300);
		Assertions.assertEquals(frozen, rounds.get(), "cancel后不得再触发新一轮");
		Assertions.assertTrue(capture.messages.stream().noneMatch(m -> m.contains("fastNoWarn")),
				"快路径不得发出挂死告警，实际=" + capture.messages);
	}
}
