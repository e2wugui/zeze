package Zeze.Util;

import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND15 util-03 守护钉板：cancelScheduledLog 的"锁内捕获句柄、解锁后再 cancel"改造
 * 不得破坏停启语义。ABBA 死锁本体（cancel 撞上在飞周期 tick）需 100s 周期首跳+调度交叠，
 * 无法确定性构造（对齐 FND13 svc-02 口径：编译+机制核查），本钉板钉 benign 路径：
 * 停止→重启动作产出新 future、旧 future 确实取消、重复停止幂等返回 false。
 */
@Fast
public class TestFnd15Util03PerfCounterCancelScheduledLog {
	static {
		Task.tryInitThreadPool();
	}

	@Test
	public void testStopRestartSemantics() {
		var pc = new PerfCounter();
		var f1 = pc.tryStartScheduledLog();
		Assertions.assertNotNull(f1, "首次启动应创建调度");
		Assertions.assertFalse(f1.isCancelled());

		Assertions.assertTrue(pc.cancelScheduledLog(), "停止在飞的调度应返回true");
		Assertions.assertTrue(f1.isCancelled(), "旧future必须已取消");
		Assertions.assertFalse(pc.cancelScheduledLog(), "重复停止（已置空）幂等返回false");

		var f2 = pc.tryStartScheduledLog();
		Assertions.assertNotSame(f1, f2, "停止后重启必须创建新调度");
		Assertions.assertFalse(f2.isCancelled());
		// 收尾：不留周期任务
		pc.cancelScheduledLog();
	}
}
