package UnitTest.Zeze.Util;

import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * U5-F2：get(timeout) 的 deadline 加法在 toNanos 饱和值上溢出为负的 deadline（不变式破坏）。
 * <p>
 * 修复：timeout &gt;= Long.MAX_VALUE - System.nanoTime() 时钳制 deadline 为 Long.MAX_VALUE
 * （对齐 j.u.c 对饱和超时值的"不超时"特判）。说明：原实现里 deadline-now 的补码双重回绕
 * 在模运算上恰好自愈，伪超时实测不可达（修复前本测试同样通过）——本测试的价值是钉住
 * "饱和超时 = 等到结果为止"的契约，防止后续改动把 deadline 当真值比较或复用未钳制的
 * 剩余时间计算时引入真回归。
 */
@Fast
public class TestTaskCompletionSourceSaturatedTimeout {

	/**
	 * 陈旧 unpark 许可令首轮 parkNanos 立即返回：修复前 deadline 回绕为负，
	 * 复查时 timeout<=0 抛 TimeoutException；修复后继续等到结果。
	 */
	@Test
	@Timeout(10)
	public void testSaturatedTimeoutWaitsForResult() throws InterruptedException {
		var tcs = new TaskCompletionSource<String>();
		var setter = new Thread(() -> {
			try {
				Thread.sleep(200);
			} catch (InterruptedException ignored) {
			}
			tcs.setResult("ok");
		});
		setter.start();
		LockSupport.unpark(Thread.currentThread()); // 制造陈旧许可
		assertEquals("ok", tcs.get(Long.MAX_VALUE - 1_000_000L, TimeUnit.NANOSECONDS));
		setter.join();
	}

	/** 已完成的结果在饱和超时下立即返回（不进 park 路径也不受影响）。 */
	@Test
	@Timeout(10)
	public void testSaturatedTimeoutCompletedNow() {
		var tcs = new TaskCompletionSource<String>();
		tcs.setResult("done");
		assertEquals("done", tcs.get(Long.MAX_VALUE - 1_000_000L, TimeUnit.NANOSECONDS));
	}

	/** 常规小超时语义不变：仍按期超时。 */
	@Test
	@Timeout(10)
	public void testNormalTimeoutStillThrows() {
		var tcs = new TaskCompletionSource<String>();
		assertThrows(TimeoutException.class, () -> tcs.get(50, TimeUnit.MILLISECONDS));
	}
}
