package UnitTest.Zeze.Util;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * call() 先校验后消费（FND2-U0-2）：
 * call() 原先先 consume() 再检查 dispatchMode/timeout/onCancel，校验抛 IAE 后
 * 实例已失效，文档承诺的补救调用(call失败→runNow)被 IllegalStateException 吃掉。
 * 修复后校验移到 consume() 之前（与 run/schedule/executeOneByOne 全族对齐）。
 */
@Fast
public class TestTaskSpecCallRecover {

	@org.junit.jupiter.api.BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testCallRejectsWithoutConsuming() throws InterruptedException {
		var count = new AtomicInteger();
		var done = new java.util.concurrent.CountDownLatch(1);
		var spec = TaskSpec.ofAction(() -> {
					count.incrementAndGet();
					done.countDown();
				})
				.name("testCallRejectsWithoutConsuming")
				.dispatchMode(DispatchMode.Critical); // call() 不消费的选项
		// 校验失败抛 IAE，且实例未失效
		Assertions.assertThrows(IllegalArgumentException.class, spec::call);
		// 补救路径可用：修复前此处抛 IllegalStateException("has been consumed")。
		// runNow 异步入池，用闭锁等动作真实执行后再断言。
		spec.runNow();
		Assertions.assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS), "action must run");
		Assertions.assertEquals(1, count.get());
		// 补救调用消费实例后，再次终结调用仍被拒绝：call() 先查选项（IAE）后查 consumed（ISE），
		// 此处 dispatchMode 仍在设置中，故抛 IAE——两种异常都证明实例不可再终结。
		Assertions.assertThrows(IllegalArgumentException.class, spec::call);
	}
}
