package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskOneByOneBase;
import Zeze.Util.TaskOneByOneQueue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-02回归：executeCyclicBarrier逐桶提交循环中submit/派发抛出（池未初始化ISE、
 * 自定义executor拒绝REE）时异常直接逃出循环，group中未轮到桶的队列锁永久泄漏
 * （路由到该桶的调用永久阻塞、shutdown/waitComplete永挂），且已提交桶的barrier
 * 任务count永不归零、队列楔死。修复：逐桶try-catch——补解锁剩余桶、barrier.cancel()
 * （幂等）后重抛首个异常。确定性：全程同步执行，无时序依赖；HashMap桶序不定，
 * 断言不依赖处理顺序。
 */
@Fast
public class TestFnd802CyclicBarrierSubmitFailUnlock {

	/** 桶队列按key下标路由的极简桩：getAndLockQueue返回已加锁队列（对齐ByKey实现）。 */
	private static final class Rig extends TaskOneByOneBase {
		private final TaskOneByOneQueue[] queues;

		Rig(TaskOneByOneQueue... queues) {
			this.queues = queues;
		}

		@Override
		protected TaskOneByOneQueue getAndLockQueue(Object key) {
			var q = queues[(int)(long)(Long)key % queues.length];
			q.lock();
			return q;
		}
	}

	/** 模拟submit入口抛ISE（池未初始化/停机窗口poolOrThrow形态）：任务未入队，
	 * 无回滚自补偿，barrier补偿全靠executeCyclicBarrier的失败处理。 */
	private static final class SubmitThrowQueue extends TaskOneByOneQueue {
		SubmitThrowQueue() {
			super(Runnable::run);
		}

		@Override
		public Runnable submit(Task task) {
			throw new IllegalStateException("a1_fnd802 submit throw");
		}
	}

	/** submit抛出（路径一，任务未入队）：剩余桶补解锁、屏障取消、首异常重抛。
	 * 两个桶都会抛，无论HashMap先处理哪个，另一个都是"未轮到的泄漏桶"。 */
	@Test
	public void testSubmitThrowProcedureOverload() {
		var q0 = new SubmitThrowQueue();
		var q1 = new SubmitThrowQueue();
		var rig = new Rig(q0, q1);
		var procedure = new Procedure((Application)null, (Zeze.Util.FuncLong)() -> {
			throw new AssertionError("barrier被取消，procedure不得执行");
		}, "a1_fnd802", null);
		var cancelRuns = new AtomicInteger();
		var keys = new ArrayList<>(List.of(0L, 1L)); // 不同key落不同桶

		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> rig.executeCyclicBarrier(keys, procedure, cancelRuns::incrementAndGet, null));
		Assertions.assertEquals("a1_fnd802 submit throw", ex.getMessage());

		// 修复前红：未轮到的桶队列锁泄漏；同线程重入探不出来，用isLocked
		Assertions.assertFalse(q0.isLocked(), "bucket queue lock must not leak");
		Assertions.assertFalse(q1.isLocked(), "bucket queue lock must not leak");
		Assertions.assertEquals(1, cancelRuns.get(), "barrier必须取消（cancel补偿钩子恰好执行一次）");
	}

	/** 派发拒绝（路径二，REE已回滚自补偿屏障，剩余桶锁补偿是唯一新增）：第一个提交的桶
	 * 内联驱动（barrier任务reach后stall等待其余桶），第二个桶REE，第三个桶未轮到。 */
	@Test
	public void testDispatchRejectUnlocksRestAndRescuesSubmittedBucket() {
		var dispatches = new AtomicInteger();
		var executor = new java.util.concurrent.Executor() {
			@Override
			public void execute(Runnable r) {
				if (dispatches.incrementAndGet() == 1)
					r.run(); // 第一个桶：内联驱动，barrier任务reach()返回false后stall
				else
					throw new RejectedExecutionException("a1_fnd802 reject");
			}
		};
		var q0 = new TaskOneByOneQueue(executor);
		var q1 = new TaskOneByOneQueue(executor);
		var q2 = new TaskOneByOneQueue(executor);
		var rig = new Rig(q0, q1, q2);
		var actionRuns = new AtomicInteger();
		var cancelRuns = new AtomicInteger();
		var keys = new ArrayList<>(List.of(0L, 1L, 2L));

		Assertions.assertThrows(RejectedExecutionException.class,
				() -> rig.executeCyclicBarrier(keys, "a1_fnd802", (Zeze.Util.Action0)actionRuns::incrementAndGet,
						cancelRuns::incrementAndGet, null));

		// 修复前红：未轮到的桶队列锁泄漏
		Assertions.assertFalse(q0.isLocked(), "bucket queue lock must not leak");
		Assertions.assertFalse(q1.isLocked(), "bucket queue lock must not leak");
		Assertions.assertFalse(q2.isLocked(), "bucket queue lock must not leak");
		// 已提交桶被barrier.cancel()收尾：队列清空，不再楔死
		Assertions.assertEquals(0, q0.size());
		Assertions.assertEquals(0, q1.size());
		Assertions.assertEquals(0, q2.size());
		Assertions.assertEquals(0, actionRuns.get(), "屏障未到齐，action不得执行");
		Assertions.assertEquals(1, cancelRuns.get(), "cancel补偿钩子恰好执行一次（幂等）");
	}
}
