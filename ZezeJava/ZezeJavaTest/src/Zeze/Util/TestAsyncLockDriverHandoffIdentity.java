package Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * 拒绝兜底驱动交接时，旧驱动finally无条件清inlineDriverThread抹掉新代标记。
 * <p>
 * 交错：旧驱动A在队列耗尽边界发布state=0后（尚未退出driveRejectedInline），新线程B
 * CAS接管派发并因池拒绝进入新的兜底驱动，写入自己的标记；A的finally随后无条件置null
 * 抹掉B的标记。B的回调显式leave()时因标记丢失误判"非内联驱动"而派发tryNextAsync——
 * 池已恢复则后续回调提交到池，与B的内联循环继续poll的回调并发执行，互斥破坏。
 * <p>
 * 修复：finally按驱动身份CAS清理（标记仍属于本线程才清null；先读后写的非原子判断同样
 * 存在覆盖新代标记的窗口）。测试钩子testHookAtInlineDriverEmptyBoundary把A确定性停在
 * "state=0已发布、驱动尚未退场"的交接窗口，复现审核报告的固定交错。
 * <p>
 * 本类会真实关闭全局池制造确定性拒绝，@Isolated独占运行，AfterEach重建（与
 * TestAsyncLockDispatchReject相同）。核心断言：任意时刻只有一个持锁回调在执行
 * （修复前红：cb4（池线程）与cb5（B内联）重叠，maxActive==2）。
 */
@Fast
@Isolated
public class TestAsyncLockDriverHandoffIdentity {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		// 恢复全局池。注意：重建的是新池，原池上注册的静态周期任务不会恢复。
		Task.tryInitThreadPool();
	}

	@Test
	public void testOldDriverFinallyMustNotClearNewDriverMarker() throws Exception {
		// 关池：所有tryNextAsync派发被拒，驱动进入内联兜底（确定性拒绝路径）。
		try {
			Task.shutdown(200);
		} catch (java.util.concurrent.TimeoutException expected) {
			// 全套件环境下其他测试类遗留的周期任务令终止等待超时，忽略。
		}

		var lock = new AsyncLock(); // 异步派发模式
		var cb1Inside = new CountDownLatch(1);
		var cb1Free = new CountDownLatch(1);
		var cb2Ran = new CountDownLatch(1);
		var hookEntered = new CountDownLatch(1);
		var hookFree = new CountDownLatch(1);
		var cb3Started = new CountDownLatch(1);
		var aFinallyDone = new CountDownLatch(1);
		var cb4Ran = new CountDownLatch(1);
		var cb5Ran = new CountDownLatch(1);
		var active = new AtomicInteger();
		var maxActive = new AtomicInteger();

		// A：快路径占住派发权（cb1临界区内等待）。
		var thrown = new AtomicReference<Throwable>();
		var a = new Thread(() -> {
			try {
				lock.enter(() -> {
					cb1Inside.countDown();
					awaitQuiet(cb1Free);
				});
			} catch (Throwable e) {
				thrown.compareAndSet(null, e);
			}
		}, "asynclock-old-driver");
		a.setDaemon(true);
		a.start();
		Assertions.assertTrue(cb1Inside.await(5, TimeUnit.SECONDS), "A必须先占住派发权");

		// cb2趁窗口入队（只入队不派发），随后挂上空边界钩子并放行cb1：
		// A的leave→tryNextAsync派发cb2被拒→A成为内联驱动(marker=A)；cb2执行完后
		// 队列耗尽，state=0发布后A停在钩子——正是交接窗口。
		lock.enter(cb2Ran::countDown);
		lock.testHookAtInlineDriverEmptyBoundary = () -> {
			hookEntered.countDown();
			awaitQuiet(hookFree);
		};
		cb1Free.countDown();
		Assertions.assertTrue(cb2Ran.await(5, TimeUnit.SECONDS), "cb2必须被A内联执行");
		Assertions.assertTrue(hookEntered.await(5, TimeUnit.SECONDS), "A必须停在空边界钩子上");

		// B：接管派发（state==0，CAS快路径）。cbB1内嵌套入队cb3（B持state==1只入队），
		// cbB1收尾的leave派发cb3被拒→B成为新内联驱动(marker=B)，cb3内联执行；
		// cb3再嵌套入队cb4/cb5，等A的finally走完后显式leave——旧代码此时标记已被抹掉。
		var b = new Thread(() -> lock.enter(() -> lock.enter(() -> {
			lock.enter(() -> {
				gaugeEnter(active, maxActive);
				cb4Ran.countDown();
				sleepQuietly(50);
				active.decrementAndGet();
			});
			lock.enter(() -> {
				gaugeEnter(active, maxActive);
				cb5Ran.countDown();
				sleepQuietly(50);
				active.decrementAndGet();
			});
			cb3Started.countDown();
			awaitQuiet(aFinallyDone);
			lock.leave(); // 显式释放：修复前误启第二条派发链（cb4提交到池），与B循环并发
		})), "asynclock-new-driver");
		b.setDaemon(true);
		b.start();
		Assertions.assertTrue(cb3Started.await(5, TimeUnit.SECONDS), "cb3必须开始执行（B已是内联驱动）");

		// 放行A：A回到循环——队列非空(cb4,cb5)但CAS失败（B持state==1）→A退场，
		// finally按身份清理：标记仍为B，不得清null。join确保A的finally已执行完。
		hookFree.countDown();
		a.join(5000);
		Assertions.assertFalse(a.isAlive(), "旧驱动A必须退出");
		Assertions.assertNull(thrown.get(), "A不得抛异常");

		// 池恢复后放行cb3：修复前leave把cb4提交到池、与B内联循环的cb5并发（红）；
		// 修复后leave被抑制，B的循环串行内联执行cb4、cb5。
		Task.tryInitThreadPool();
		aFinallyDone.countDown();

		b.join(10_000);
		Assertions.assertFalse(b.isAlive(), "新驱动B必须排空队列退出");
		Assertions.assertEquals(0, cb4Ran.getCount(), "cb4必须执行");
		Assertions.assertEquals(0, cb5Ran.getCount(), "cb5必须执行");
		Assertions.assertEquals(1, maxActive.get(), "任意时刻必须只有一个持锁回调在执行（互斥核心断言）");
		Assertions.assertFalse(lock.isLocked(), "驱动退场后state必须释放");
	}

	private static void gaugeEnter(AtomicInteger active, AtomicInteger maxActive) {
		var now = active.incrementAndGet();
		maxActive.accumulateAndGet(now, Math::max);
	}

	private static void awaitQuiet(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS))
				throw new IllegalStateException("latch timeout");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
