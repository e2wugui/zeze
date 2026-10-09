package Zeze;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * halt前的尽力保存不得决定终止时机：释放超时触发halt时，同步checkpoint可被
 * 记录集锁/后端IO无限阻塞，等它即无限延后halt——GCM租约到期后他节点已接管，
 * 本进程继续存活会把旧脏数据刷入共享库。独立watchdog保证截止时间必达：
 * 主线程尽力保存，超时由watchdog直接执行终止动作（宁可舍弃来不及保存的脏值）。
 */
@Fast
public class TestHaltDeadlineNotBlockedByCheckpoint {

	/** 尽力保存永久阻塞（锁/IO不返回形态）：截止时间到终止动作必须执行。 */
	@Test
	public void blockedBestEffortStillHaltsWithinDeadline() throws Exception {
		var halted = new AtomicInteger();
		var release = new CountDownLatch(1);
		long start = System.currentTimeMillis();
		Application.haltWithDeadline(200, () -> {
			try {
				release.await(10, TimeUnit.SECONDS); // 模拟checkpointRun永久阻塞
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, () -> {
			halted.incrementAndGet();
			release.countDown(); // 终止动作已发生：放行主线程收尾（真实halt下进程已退出）
		});
		long elapsed = System.currentTimeMillis() - start;
		Assertions.assertEquals(1, halted.get(), "阻塞的尽力保存不得阻止终止（由watchdog按截止执行，且恰一次）");
		assertTrue(elapsed < 5000, "终止不得被尽力保存显著拖过截止（实测" + elapsed + "ms，截止200ms）");
	}

	/** 尽力保存正常完成：主线程执行终止动作，watchdog撤销不重复执行。 */
	@Test
	public void completedBestEffortHaltsOnMainThreadOnce() throws Exception {
		var halted = new AtomicInteger();
		Application.haltWithDeadline(10_000, () -> {
		}, halted::incrementAndGet);
		Assertions.assertEquals(1, halted.get(), "尽力保存完成后必须执行终止动作");
		Thread.sleep(300); // watchdog应已被撤销，不得迟到补枪
		Assertions.assertEquals(1, halted.get(), "终止动作不得重复执行");
	}
}
