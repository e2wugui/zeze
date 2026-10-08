package Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.TaskOneByOneByKey2;

/**
 * FND12 util-03回归：TaskOneByOneByKey2.executeCyclicBarrier 桶派发失败无补偿清理。
 * 任一桶提交失败（池未初始化ISE/自定义executor拒绝REE）时异常裸传：barrier 计数永不归零，
 * 已入队并已 reach 的桶 submitted 认领永久为 true——该桶（及其哈希映射的全部 key）永久
 * 停摆并无界增长。base 家族有完善失败清理+cancel，Key2 移植遗漏。
 * 修复：Barrier 补内部 canceled 语义——reach 见 canceled 返回 true（任务照常消费），
 * cancel 解冻已 reach 的桶；executeCyclicBarrier 失败即 cancel 后重抛首个异常。
 * <p>
 * 复现：自定义 executor 首次 execute 抛 REE（此后内联直跑，单线程确定序）。多 key 跨桶
 * 屏障（concurrencyLevel=4）首桶派发被拒：断言 REE 重抛、屏障回调不执行；随后向同批 key
 * 提交普通任务，修复前被毒桶队列头的屏障任务 reach 返回 false 押死认领，后续任务永不执行
 * （计数器短缺，断言失败）；修复后屏障任务变为 no-op 继续消费，全部任务完成。
 */
@Fast
public final class TestTaskOneByOneByKey2BarrierCancel {
	// 首次派发拒绝、此后内联直跑：保证失败窗口确定且后续驱动单线程确定序。
	private static final class RejectOnceExecutor implements java.util.concurrent.Executor {
		boolean rejected;

		@Override
		public void execute(Runnable command) {
			if (!rejected) {
				rejected = true;
				throw new RejectedExecutionException("reject once (FND12 util-03)");
			}
			command.run();
		}
	}

	@Test
	public void testDispatchFailureCancelsBarrierAndUnfreezesBuckets() {
		var executor = new RejectOnceExecutor();
		var key2 = new TaskOneByOneByKey2(4, executor);
		var barrierRan = new AtomicBoolean();
		var ranCount = new AtomicInteger();

		// 多 key 跨桶：首个提交桶的派发被拒，屏障失败并重抛首个异常。
		List<Integer> keys = new ArrayList<>();
		for (int k = 0; k < 64; k++)
			keys.add(k);
		assertThrows(RejectedExecutionException.class, () -> key2.executeCyclicBarrier(keys,
				"ByKey2Barrier", () -> barrierRan.set(true), null));
		assertFalse(barrierRan.get(), "派发失败的屏障回调不得执行");

		// 同批 key 提交普通任务：全部同步完成（内联 executor），被毒桶不被冻结。
		// 修复前：毒桶队列头的屏障任务押死认领，该桶映射的 key 计数短缺。
		for (int k = 0; k < 64; k++) {
			final var kk = k;
			key2.Execute(kk, () -> {
				ranCount.incrementAndGet();
				return null;
			});
		}
		assertEquals(64, ranCount.get(), "屏障失败后全部桶必须继续可用（FND12 util-03）");
	}
}
