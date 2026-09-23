package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimerFuture;
import harness.Fast;

/**
 * TimerFuture body内自调cancel钉板：body在TimerFuture锁内执行，同线程cancel走ReentrantLock
 * 重入——"自调不死锁"目前靠该实现细节偶然成立，此处固化：将来若给cancel叠加非重入的等待
 * （如join在飞轮的新机制），本测试立即转红。
 */
@Fast
public class TestTimerFutureSelfCancel {

	@BeforeAll
	public static void setUp() {
		Task.tryInitThreadPool();
	}

	@AfterAll
	public static void tearDown() {
		// 池由fast套件共享，不在此关闭
	}

	@Test
	@Timeout(30)
	public void testSelfCancelReturnsAndStopsRounds() throws Exception {
		var ref = new AtomicReference<TimerFuture<Long>>();
		var runs = new AtomicInteger();
		var bodyReturned = new CountDownLatch(1);
		var future = TaskSpec.ofAction(() -> {
					runs.incrementAndGet();
					Assertions.assertTrue(ref.get().cancel(false), "首轮自调cancel必须成功");
					bodyReturned.countDown(); // cancel返回后body正常走完（挂死由@Timeout打红）
				}).name("UnitTest.TimerFuture.selfCancel")
				.schedulePeriodNow(100, 100);
		ref.set(future);
		try {
			Assertions.assertTrue(bodyReturned.await(5, TimeUnit.SECONDS), "首轮body必须在5s内完成");
			var frozen = runs.get();
			Thread.sleep(400); // 100ms周期下若未取消将多跑约3轮
			Assertions.assertEquals(frozen, runs.get(), "自cancel后不得再触发新一轮");
		} finally {
			future.cancel(false);
		}
	}
}
