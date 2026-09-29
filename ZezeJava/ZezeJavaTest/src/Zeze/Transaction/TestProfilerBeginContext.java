package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertTrue;

import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-01回归：beginContext丢失count=n+1。
 * beginContext把上下文写入槽位n=count后不推进count（onRedo有推进），toString/genInfo
 * 只迭代[0,count)：begin登记的上下文全部不可见；同事务多次begin互相覆写同一槽位，
 * onRedo的REDO标记再覆写begin占用的槽位——慢事务剖析输出只剩REDO标记，
 * RecordWaitFairLock、Lockey锁等待、用户埋点全部丢失（88aeceec1回归）。
 * 修复：恢复count=n+1。测试：begin登记的上下文必须出现在toString输出里，
 * onRedo不得覆写begin已占用的槽位，reset清空后输出为空。
 */
@Fast
public class TestProfilerBeginContext {

	@Test
	public void testBeginContextVisibleInOutput() throws Exception {
		var p = new Profiler();
		var startTimeField = Profiler.class.getDeclaredField("startTime");
		startTimeField.setAccessible(true);
		startTimeField.setLong(p, System.nanoTime());

		var beginContext = Profiler.class.getDeclaredMethod("beginContext", Object.class);
		beginContext.setAccessible(true);

		try (var ignored = (Profiler.Context)beginContext.invoke(p, "outer")) {
			try (var ignored2 = (Profiler.Context)beginContext.invoke(p, "inner")) {
				Thread.sleep(30); // 拉开时间段
			}
		}

		// 核心（红）：begin登记的上下文必须计入count并出现在输出——修复前count不推进，
		// toString为空串，outer/inner全部丢失
		var s = p.toString();
		assertTrue(s.contains("outer"), "begin登记的outer上下文必须出现在剖析输出：\n" + s);
		assertTrue(s.contains("inner"), "begin登记的inner上下文必须出现在剖析输出：\n" + s);

		// REDO追加不得覆写begin已占用的槽位（修复前onRedo写在count槽位=覆写最后一个begin）
		p.onRedo();
		var s2 = p.toString();
		assertTrue(s2.contains("outer") && s2.contains("inner") && s2.contains("REDO"),
				"onRedo必须追加而非覆写begin的槽位：\n" + s2);

		// reset后输出为空（顺带：count推进后reset清理覆盖全部已写槽位）
		p.reset();
		assertTrue(p.toString().isEmpty(), "reset后剖析输出必须为空");
	}
}
