package UnitTest.Zeze.Util;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.TaskOneByOneQueue;
import Zeze.Util.TaskOneByOneQueue.BarrierAction;
import Zeze.Util.TaskOneByOneQueue.TaskBarrierAction;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND11 util-04回归：Barrier.reach/cancel曾持屏障锁调reachedRunNext推进各桶——runNext尾部
 * 派发失败回滚（停机先置空池/自定义执行器拒绝）对整队执行cancel补偿，队内其他屏障任务
 * 的cancel即对方屏障锁，两线程各持一屏障锁经回滚反向互等：永久死锁、涉及桶waitComplete
 * 永等。修复后reachedRunNext锁内快照清空、锁外推进，锁链在第一环斩断。
 * 确定性：cancelAction里CyclicBarrier会合保证两线程同时持各自屏障锁下降进入回滚
 * （修复前必成环楔死，修复后必完成）。
 */
@Fast
public class TestFnd11Util04BarrierCrossRollbackDeadlock {

	/** 前allow次派发内联执行、之后恒拒绝：首派发驱动batch到达barrier停等，
	 * runNext的再派发被拒触发回滚吃掉对方屏障任务。 */
	private static final class RejectAfter implements Executor {
		private final int allow;
		private final AtomicInteger served = new AtomicInteger();

		RejectAfter(int allow) {
			this.allow = allow;
		}

		@Override
		public void execute(Runnable r) {
			if (served.incrementAndGet() > allow)
				throw new RejectedExecutionException("test reject #" + served.get());
			r.run();
		}
	}

	@Test
	public void testCrossRollbackNoDeadlock() throws Exception {
		// b1任务：Q1认领区（已到达，batch停等）+Q3未认领（回滚驱动）；b2对称：Q2+Q4。
		// Q1/Q2首派发成功（内联驱动reach，count 2→1停等）、再派发拒绝；Q3/Q4首派发即拒。
		var q1 = new TaskOneByOneQueue(new RejectAfter(1));
		var q2 = new TaskOneByOneQueue(new RejectAfter(1));
		var q3 = new TaskOneByOneQueue(new RejectAfter(0));
		var q4 = new TaskOneByOneQueue(new RejectAfter(0));

		var rendezvous = new CyclicBarrier(2);
		var b1CancelRan = new AtomicBoolean();
		var b2CancelRan = new AtomicBoolean();
		var b1RunRan = new AtomicBoolean();
		var b2RunRan = new AtomicBoolean();
		var b1 = new BarrierAction("b1", () -> b1RunRan.set(true), 2, () -> {
			b1CancelRan.set(true);
			rendezvous.await(10, TimeUnit.SECONDS);
		});
		var b2 = new BarrierAction("b2", () -> b2RunRan.set(true), 2, () -> {
			b2CancelRan.set(true);
			rendezvous.await(10, TimeUnit.SECONDS);
		});

		// Q1: [b1a(认领到达), b2x(未认领)]；Q2: [b2a(认领到达), b1x(未认领)]
		var r1 = q1.submit(new TaskBarrierAction(b1, 1, null));
		Assertions.assertNotNull(r1);
		r1.run();
		Assertions.assertNull(q1.submit(new TaskBarrierAction(b2, 1, null)));
		var r2 = q2.submit(new TaskBarrierAction(b2, 1, null));
		Assertions.assertNotNull(r2);
		r2.run();
		Assertions.assertNull(q2.submit(new TaskBarrierAction(b1, 1, null)));

		// 驱动链：Q3派发被拒→回滚b1b→b1.cancel（持L(b1)，cancelAction会合）；Q4对称持L(b2)。
		// 会合释放后双双下降：各自reachedRunNext→对方队列回滚→对方屏障cancel。
		var r3 = q3.submit(new TaskBarrierAction(b1, 1, null));
		Assertions.assertNotNull(r3);
		var r4 = q4.submit(new TaskBarrierAction(b2, 1, null));
		Assertions.assertNotNull(r4);
		var rethrows = new AtomicInteger();
		var a = new Thread(() -> {
			try {
				r3.run();
			} catch (RuntimeException e) {
				rethrows.incrementAndGet();
			}
		}, "fnd11-util04-a");
		var b = new Thread(() -> {
			try {
				r4.run();
			} catch (RuntimeException e) {
				rethrows.incrementAndGet();
			}
		}, "fnd11-util04-b");
		a.setDaemon(true);
		b.setDaemon(true);
		a.start();
		b.start();
		a.join(15_000);
		b.join(15_000);
		Assertions.assertFalse(a.isAlive(), "线程A楔死：Q3回滚→b1.cancel持锁推进→Q1回滚→b2::cancel等L(b2)，与B反向互等");
		Assertions.assertFalse(b.isAlive(), "线程B楔死（对称）");
		Assertions.assertTrue(b1CancelRan.get());
		Assertions.assertTrue(b2CancelRan.get());
		Assertions.assertFalse(b1RunRan.get(), "count未归零，run不得执行");
		Assertions.assertFalse(b2RunRan.get(), "count未归零，run不得执行");
		Assertions.assertEquals(2, rethrows.get(), "两条驱动链的派发拒绝按设计重抛");
		for (var q : new TaskOneByOneQueue[]{q1, q2, q3, q4}) {
			Assertions.assertEquals(0, q.size());
			q.waitComplete(); // 修复前涉及桶waitComplete永等
		}
	}
}
