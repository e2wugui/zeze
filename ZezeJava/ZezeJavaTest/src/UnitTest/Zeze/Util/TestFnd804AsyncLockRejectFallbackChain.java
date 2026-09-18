package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Util.AsyncLock;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND8-04回归：AsyncLock.rollbackRejectedDispatch复位state后不复查就绪队列，与并发
 * enter的offer后重试CAS竞态会把回调滞留成无派发者真空（state==0、队列非空、无人
 * 派发，直到同key下一个enter才自愈）；且回滚本身就滞留到下一个enter。修复：派发
 * 被拒时不再回滚复位，就地内联执行runWithLeave续走整个派发链（嵌套深度≤队列长）。
 * 本类会真实关闭全局池，@Isolated 独占运行，AfterEach 重建（与 TestTaskShutdown 相同）。
 */
@Fast
@Isolated
public class TestFnd804AsyncLockRejectFallbackChain {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		// 恢复全局池。注意：重建的是新池，原池上注册的静态周期任务不会恢复。
		Task.tryInitThreadPool();
	}

	/** 停机池上派发被拒：积压的多个回调经内联回退逐个执行完毕，不滞留、不楔死、不重抛。
	 * 修复前红：cb2被重排队列滞留到下一个enter（cb3在池恢复前永远不执行）。 */
	@Test
	public void testRejectFallsBackWholeChain() throws Exception {
		shutdownIgnoringTerminationTimeout();

		var lock = new AsyncLock(); // 异步派发模式
		var holderInside = new CountDownLatch(1);
		var releaseHolder = new CountDownLatch(1);
		var cb2Ran = new CountDownLatch(1);
		var cb3Ran = new CountDownLatch(1);
		// 内联执行线程上看到的执行顺序记录（cb2必须先于cb3，且都在holder线程上）
		var order = new AtomicInteger(0);
		var cb2Order = new AtomicInteger(-1);
		var cb3Order = new AtomicInteger(-1);

		var holder = new Thread(() -> {
			lock.enter(() -> {
				holderInside.countDown();
				try {
					releaseHolder.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}, "fnd804-holder");
		holder.setDaemon(true);
		holder.start();
		Assertions.assertTrue(holderInside.await(5, TimeUnit.SECONDS), "holder必须先占住派发权");

		// cb2、cb3趁窗口只入队（派发权在holder手里，enter的两次CAS必失败）
		var enqueuer2 = new Thread(() -> lock.enter(() -> {
			cb2Order.set(order.incrementAndGet());
			cb2Ran.countDown();
		}), "fnd804-enqueuer2");
		enqueuer2.setDaemon(true);
		enqueuer2.start();
		enqueuer2.join(5000);
		var enqueuer3 = new Thread(() -> lock.enter(() -> {
			cb3Order.set(order.incrementAndGet());
			cb3Ran.countDown();
		}), "fnd804-enqueuer3");
		enqueuer3.setDaemon(true);
		enqueuer3.start();
		enqueuer3.join(5000);

		// holder返回：leave→tryNextAsync在停机池上派发cb2被拒→内联执行cb2→其leave
		// →tryNextAsync再被拒→内联执行cb3——整个派发链就地续走收尾
		releaseHolder.countDown();
		holder.join(5000);
		Assertions.assertFalse(holder.isAlive(), "holder必须在5秒内结束");

		Assertions.assertTrue(cb2Ran.await(5, TimeUnit.SECONDS), "积压cb2必须内联执行，不得滞留");
		Assertions.assertTrue(cb3Ran.await(5, TimeUnit.SECONDS), "积压cb3必须内联执行，不得滞留");
		Assertions.assertTrue(cb2Order.get() < cb3Order.get(), "串行语义：cb2先于cb3执行");
		Assertions.assertFalse(lock.isLocked(), "派发链续走收尾后state必须释放");

		// 池恢复后新enter照常
		Task.tryInitThreadPool();
		var cb4Ran = new CountDownLatch(1);
		lock.enter(cb4Ran::countDown);
		Assertions.assertTrue(cb4Ran.await(5, TimeUnit.SECONDS));
	}

	// 同 TestTaskShutdown：短等待关池（先置 null 再等待），终止超时忽略——
	// 用例只依赖"默认池字段已 null"，不依赖遗留任务全部结束。
	private static void shutdownIgnoringTerminationTimeout() throws InterruptedException {
		try {
			Task.shutdown(200);
		} catch (java.util.concurrent.TimeoutException expected) {
			// 全套件环境下其他测试类遗留的周期任务令终止等待超时，忽略。
		}
	}
}
