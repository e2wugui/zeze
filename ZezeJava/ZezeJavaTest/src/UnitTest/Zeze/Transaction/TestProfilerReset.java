package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import Zeze.Transaction.Profiler;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * T3-F3 回归：Profiler.reset() 原先以 count!=0 为门，onProcedureBegin 置位过
 * startTime 但全程无 Context（count==0）时 startTime 残留，随池化 Transaction
 * 污染同线程下一个事务的诊断输出。修复：reset() 无条件清零两个状态。
 * Profiler 构造器为包私有，测试通过反射构造与检查（字段无访问器）。
 */
@Fast
public class TestProfilerReset {
	@Test
	public void testResetClearsStartTimeWhenCountZero() throws Exception {
		var ctor = Profiler.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		var profiler = ctor.newInstance();

		var startTime = Profiler.class.getDeclaredField("startTime");
		startTime.setAccessible(true);

		// 模拟"onProcedureBegin 已置位 startTime，但事务全程无 Context"的残留。
		startTime.setLong(profiler, 123456789L);
		profiler.reset();
		assertEquals(0L, startTime.getLong(profiler),
				"count==0 时 reset 也必须复位 startTime（不得随池化Transaction残留）");

		// 幂等：再次 reset 无副作用。
		profiler.reset();
		assertEquals(0L, startTime.getLong(profiler));
	}
}
