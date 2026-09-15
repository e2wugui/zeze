package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Util.TaskOneByOneQueue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-13 回归：TaskOneByOneQueue.submit在锁内校验executor非空、入队并认领
 * （size==1），返回的lambda在锁外execute——与停机（Task.shutdownPools先置
 * null再shutdownNow）或自定义池拒绝并发时抛RejectedExecutionException，
 * 队列非空且已认领、再无runNext派发点：后续submit全走size!=1分支返回null，
 * 该桶永久卡死，waitComplete永等（FND3-14修复的残余窗口）。
 * 修复：execute被拒时回滚认领（整队回收补偿+唤醒等待者），队列回到未派发
 * 状态，后续submit可重新认领派发。
 * FND6-07 补充回归：①ISE形态的派发失败同样回滚+原类型重抛、桶可复用；
 * ②回滚/shutdown-cancel补偿在锁外执行期间（含慢cancel）必须挡住waitComplete。
 */
@Fast
public class TestTaskOneByOneRejectRecover {

	private static final class FlagTask extends TaskOneByOneQueue.Task {
		final AtomicBoolean ran;

		FlagTask(String name, AtomicBoolean ran) {
			super(name, null, null);
			this.ran = ran;
		}

		@Override
		public boolean isBarrier() {
			return false;
		}

		@Override
		public boolean process(TaskOneByOneQueue.BatchTask batch) {
			ran.set(true);
			return true;
		}
	}

	@Test
	public void testRejectThenRecover() throws Exception {
		var rejecting = new AtomicBoolean(true);
		var t1Ran = new AtomicBoolean();
		var t2Ran = new AtomicBoolean();
		var queue = new TaskOneByOneQueue(r -> {
			if (rejecting.get())
				throw new RejectedExecutionException("poison");
			r.run();
		});

		var t1 = new FlagTask("t1", t1Ran);
		var dispatch = queue.submit(t1);
		Assertions.assertNotNull(dispatch, "首个任务必须返回派发动作");
		Assertions.assertThrows(RejectedExecutionException.class, dispatch::run,
				"注入拒绝必须原样抛回调用方");

		// FND5-13核心：拒绝后桶不得卡死——后续submit必须能重新认领派发。
		rejecting.set(false);
		var t2 = new FlagTask("t2", t2Ran);
		var dispatch2 = queue.submit(t2);
		Assertions.assertNotNull(dispatch2,
				"拒绝后桶不得卡死，后续submit必须重新认领派发（FND5-13）");
		dispatch2.run();
		Assertions.assertTrue(t2Ran.get(), "恢复后任务必须执行");
		Assertions.assertFalse(t1Ran.get(), "被拒绝回收的任务不再执行");

		// waitComplete必须能收敛（修复前queue非空永等——由@Timeout兜底暴露）。
		queue.shutdown(false);
		Assertions.assertDoesNotThrow(() -> queue.waitComplete());
	}

	/** FND6-07回归①：getExecutor在停机序下抛IllegalStateException（非REE）同样必须
	 * 回滚认领、按原类型重抛（不得包装转换），回滚后桶可复用、waitComplete收敛。 */
	@Test
	public void testIllegalStateDispatchThenRecover() throws Exception {
		var throwing = new AtomicBoolean(true);
		var t1Ran = new AtomicBoolean();
		var t2Ran = new AtomicBoolean();
		var queue = new TaskOneByOneQueue(r -> {
			if (throwing.get())
				throw new IllegalStateException("pool is null (poison)");
			r.run();
		});

		var t1 = new FlagTask("t1", t1Ran);
		var dispatch = queue.submit(t1);
		Assertions.assertNotNull(dispatch, "首个任务必须返回派发动作");
		var ex = Assertions.assertThrows(IllegalStateException.class, dispatch::run,
				"注入ISE必须按原类型抛回调用方（FND6-07）");
		Assertions.assertEquals("pool is null (poison)", ex.getMessage(), "异常不得被包装转换");

		// ISE回滚后桶不得卡死——后续submit必须能重新认领派发。
		throwing.set(false);
		var t2 = new FlagTask("t2", t2Ran);
		var dispatch2 = queue.submit(t2);
		Assertions.assertNotNull(dispatch2, "ISE回滚后桶不得卡死，后续submit必须重新认领派发（FND6-07）");
		dispatch2.run();
		Assertions.assertTrue(t2Ran.get(), "恢复后任务必须执行");
		Assertions.assertFalse(t1Ran.get(), "被ISE回滚回收的任务不再执行");

		queue.shutdown(false);
		Assertions.assertDoesNotThrow(() -> queue.waitComplete());
	}

	/** FND6-07回归②：shutdown(true)后runNext收尾补偿在锁外执行（含慢cancel阻塞），
	 * 期间pendingCancelCount必须挡住waitComplete——补偿完成前等待者不得放行。 */
	@Test
	public void testSlowCancelBlocksWaitComplete() throws Exception {
		var cancelEntered = new CountDownLatch(1);
		var cancelRelease = new CountDownLatch(1);
		var t3Ran = new AtomicBoolean();
		var t3Cancelled = new AtomicBoolean();

		// 每次派发新起线程：补偿线程阻塞在latch上时，主线程得以并发探测waitComplete。
		var queue = new TaskOneByOneQueue(r -> new Thread(r, "slow-cancel-dispatch").start());

		// t3：留在认领区内未执行，shutdown(true)后由runNext的shutdown-cancel路径补偿，
		// 其cancel阻塞在latch上，撑开pendingCancelCount置位窗口。
		var t3 = new TaskOneByOneQueue.Task("t3", () -> {
			t3Cancelled.set(true);
			cancelEntered.countDown();
			cancelRelease.await(); // Action0.run允许抛Exception，直接阻塞
		}, null) {
			@Override
			public boolean isBarrier() {
				return false;
			}

			@Override
			public boolean process(TaskOneByOneQueue.BatchTask batch) {
				t3Ran.set(true);
				return true;
			}
		};
		// t2：批量执行中触发shutdown(true)——批量中断，t3留在认领区未执行，
		// 转入runNext的shutdown-cancel路径（与rollbackRejectedDispatch共用pendingCancelCount）。
		var t2 = new TaskOneByOneQueue.Task("t2", null, null) {
			@Override
			public boolean isBarrier() {
				return false;
			}

			@Override
			public boolean process(TaskOneByOneQueue.BatchTask batch) {
				queue.shutdown(true);
				return true;
			}
		};
		// t1：执行期间补交t2/t3（同mode），使后续批量一次认领两个任务。
		var t1 = new TaskOneByOneQueue.Task("t1", null, null) {
			@Override
			public boolean isBarrier() {
				return false;
			}

			@Override
			public boolean process(TaskOneByOneQueue.BatchTask batch) {
				Assertions.assertNull(queue.submit(t2));
				Assertions.assertNull(queue.submit(t3));
				return true;
			}
		};

		var dispatch = queue.submit(t1);
		Assertions.assertNotNull(dispatch, "首个任务必须返回派发动作");
		dispatch.run();
		Assertions.assertTrue(cancelEntered.await(5, TimeUnit.SECONDS), "补偿必须进入慢cancel");

		// waitComplete在补偿（latch）释放前不得返回——短超时断言阻塞。
		var done = new AtomicBoolean(false);
		var waiter = new Thread(() -> {
			try {
				queue.waitComplete();
				done.set(true);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		waiter.start();
		Thread.sleep(200);
		Assertions.assertFalse(done.get(), "慢cancel完成前waitComplete不得返回（补偿期间必须阻塞）");
		Assertions.assertFalse(t3Ran.get(), "认领区内未执行的任务不得运行");

		cancelRelease.countDown();
		waiter.join(5000);
		Assertions.assertTrue(done.get(), "latch释放后waitComplete必须返回");
		Assertions.assertTrue(t3Cancelled.get(), "t3必须被补偿（cancel执行）而非运行");
	}
}
