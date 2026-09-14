package UnitTest.Zeze.Util;

import java.util.concurrent.RejectedExecutionException;
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
}
