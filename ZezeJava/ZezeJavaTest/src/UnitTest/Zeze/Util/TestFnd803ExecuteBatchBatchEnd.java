package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.Action1;
import Zeze.Util.TaskOneByOneBase;
import Zeze.Util.TaskOneByOneByKey2;
import Zeze.Util.TaskOneByOneQueue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-03回归：executeBatch入队循环中submit/派发抛出（池未初始化ISE、executor拒绝REE、
 * 队列shutdown静默丢）时Batch计数永久悬挂，batchEnd永不执行且无任何提示。修复：
 * 每key一次性核销（Settle，CAS防双重）——Base版cancel=Settle覆盖队列侧三条丢弃路径、
 * catch补核销失败key+未尝试key；Key2版notEnqueued核销入队前校验失败、派发失败任务
 * 保留队列由运行核销、catch只核销未尝试key。确定性：全程同步执行，无时序依赖。
 */
@Fast
public class TestFnd803ExecuteBatchBatchEnd {

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

	/** 模拟submit入口抛ISE（池未初始化/停机窗口poolOrThrow形态）：任务未入队。 */
	private static final class SubmitThrowQueue extends TaskOneByOneQueue {
		SubmitThrowQueue() {
			super(Runnable::run);
		}

		@Override
		public Runnable submit(Task task) {
			throw new IllegalStateException("a1_fnd803 submit throw");
		}
	}

	/** Base版路径一：submit抛出（未入队），失败key+未尝试key核销，batchEnd不悬挂。 */
	@Test
	public void testBaseSubmitThrowCompensates() {
		var rig = new Rig(new SubmitThrowQueue(), new SubmitThrowQueue(), new SubmitThrowQueue());
		var actions = new AtomicInteger();
		var batchEnds = new AtomicInteger();

		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> rig.executeBatch(List.of(0L, 1L, 2L), (Action1<Long>)k -> actions.incrementAndGet(),
						batchEnds::incrementAndGet, null));
		Assertions.assertEquals("a1_fnd803 submit throw", ex.getMessage());
		Assertions.assertEquals(0, actions.get());
		// 修复前红：计数悬挂，batchEnd永不执行
		Assertions.assertEquals(1, batchEnds.get());
	}

	/** Base版路径三：队列isShutdown对cancel!=null的任务同步执行cancel核销——静默丢不再悬挂。 */
	@Test
	public void testBaseShutdownSilentDropCompensates() {
		var q0 = new TaskOneByOneQueue(Runnable::run);
		var q1 = new TaskOneByOneQueue(Runnable::run);
		q0.shutdown(true);
		q1.shutdown(true);
		var rig = new Rig(q0, q1);
		var actions = new AtomicInteger();
		var batchEnds = new AtomicInteger();

		rig.executeBatch(List.of(0L, 1L), (Action1<Long>)k -> actions.incrementAndGet(),
				batchEnds::incrementAndGet, null);
		Assertions.assertEquals(0, actions.get());
		// 修复前红：cancel==null任务被isShutdown分支静默丢弃，batchEnd永不执行
		Assertions.assertEquals(1, batchEnds.get());
	}

	/** Base版路径二：派发REE回滚清队（cancel核销失败key），catch幂等不双重核销，
	 * 未尝试key一次性核销；已执行的key正常计入。 */
	@Test
	public void testBaseDispatchRejectCompensates() {
		var dispatches = new AtomicInteger();
		var executor = new java.util.concurrent.Executor() {
			@Override
			public void execute(Runnable r) {
				if (dispatches.incrementAndGet() == 1)
					r.run(); // 第一个key：内联驱动执行
				else
					throw new RejectedExecutionException("a1_fnd803 reject");
			}
		};
		var rig = new Rig(new TaskOneByOneQueue(executor), new TaskOneByOneQueue(executor),
				new TaskOneByOneQueue(executor));
		var actions = new ArrayList<Long>();
		var batchEnds = new AtomicInteger();

		Assertions.assertThrows(RejectedExecutionException.class,
				() -> rig.executeBatch(List.of(0L, 1L, 2L), (Action1<Long>)actions::add,
						batchEnds::incrementAndGet, null));
		Assertions.assertEquals(List.of(0L), actions);
		Assertions.assertEquals(1, batchEnds.get());
	}

	/** Key2版：派发被拒任务保留队列（不清队），catch只核销未尝试key；保留key由后续
	 * submit重新认领派发后经运行核销，batchEnd在积压key执行后才触发（不提前不悬挂）。 */
	@Test
	public void testKey2DispatchRejectRetainedKeySettlesOnRerun() {
		var dispatches = new AtomicInteger();
		var executor = new java.util.concurrent.Executor() {
			@Override
			public void execute(Runnable r) {
				if (dispatches.incrementAndGet() == 2)
					throw new RejectedExecutionException("a1_fnd803 reject");
				r.run();
			}
		};
		var oo = new TaskOneByOneByKey2(1, executor); // 单桶：保留任务的重派发确定性
		var actions = new ArrayList<Long>();
		var batchEnds = new AtomicInteger();
		var recoveryEnds = new AtomicInteger();

		Assertions.assertThrows(RejectedExecutionException.class,
				() -> oo.executeBatch(List.of(10L, 11L, 12L), (Action1<Long>)actions::add,
						batchEnds::incrementAndGet, null));
		// k10已执行；k11保留队列待重派发；k12未尝试已被核销——batchEnd等待k11
		Assertions.assertEquals(List.of(10L), actions);
		Assertions.assertEquals(0, batchEnds.get());

		// 新提交重新认领派发：积压k11执行后计数归零触发batchEnd，其后新批任务照常执行
		oo.executeBatch(List.of(99L), (Action1<Long>)actions::add, recoveryEnds::incrementAndGet, null);
		Assertions.assertEquals(List.of(10L, 11L, 99L), actions);
		// 修复前红：k12计数悬挂，k11重派发执行后仍差1，batchEnd永不触发
		Assertions.assertEquals(1, batchEnds.get());
		Assertions.assertEquals(1, recoveryEnds.get());
	}
}
