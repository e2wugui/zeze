package UnitTest.Zeze.Services;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Services.LoginQueue;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;

/**
 * FND7-21：allocateTimer是构造器赋值的final字段，stop()取消后start()只重启两个网络
 * service、不重建定时器——stop后restart永久失去1秒分配tick与队列位置广播（排队分配
 * 只剩ProcessReportProviderLoad触发的偶发drainQueue）。附带：stop关闭timeThrottle后，
 * provider全部掉线时被重建为limit=0的实例，restart后直到provider重新上报前checkNow
 * 恒false，直通分配也被禁。
 * <p>
 * 验证（Config无acceptor/connector，start/stop不绑端口）：restart后分配tick必须是
 * 未取消的新句柄（修复前是同一个已cancel的final字段）；timeThrottle必须重置回首次
 * start状态（额度耗尽的旧实例checkNow恒false）。tick的1秒周期语义不在此等待——
 * 句柄活跃性即分配能力的充要载体，drainQueue另有测试与线上路径覆盖。
 */
@Fast
public class TestFnd721LoginQueueRestart {
	static {
		Task.tryInitThreadPool();
	}

	private static Future<?> allocateTimerOf(LoginQueue lq) throws Exception {
		var field = LoginQueue.class.getDeclaredField("allocateTimer");
		field.setAccessible(true);
		return (Future<?>)field.get(lq);
	}

	private static TimeThrottle timeThrottleOf(LoginQueue lq) throws Exception {
		var field = LoginQueue.class.getDeclaredField("timeThrottle");
		field.setAccessible(true);
		return (TimeThrottle)field.get(lq);
	}

	@Test
	public void testRestartRebuildsAllocateTickAndResetsThrottle() throws Exception {
		// maxOnlineNew=2：TimeThrottleCounter(1,2,2)的窗口额度 counter<=2 且 bandwidth<2，
		// checkNow(1)首次true、第二次false（两连调之间不会赶上1秒窗口重置）
		var lq = new LoginQueue(new Config(), 2, false);
		try {
			lq.start();
			var timer1 = allocateTimerOf(lq);
			Assertions.assertNotNull(timer1, "start后分配tick必须在位");
			Assertions.assertFalse(timer1.isCancelled(), "start后分配tick不得已取消");

			// 排干限流额度：重启前旧实例额度耗尽
			Assertions.assertTrue(timeThrottleOf(lq).checkNow(1), "首次checkNow放行");
			Assertions.assertFalse(timeThrottleOf(lq).checkNow(1), "第二次checkNow被限流（额度耗尽）");

			lq.stop();
			Assertions.assertTrue(timer1.isCancelled(), "stop必须取消分配tick");

			lq.start();
			var timer2 = allocateTimerOf(lq);
			Assertions.assertNotNull(timer2, "restart后分配tick必须重建");
			Assertions.assertFalse(timer2.isCancelled(),
					"restart后分配tick必须活跃（修复前：final字段已被cancel，永久失去分配tick与位置广播）");
			Assertions.assertNotSame(timer1, timer2, "restart必须新建tick句柄");

			Assertions.assertTrue(timeThrottleOf(lq).checkNow(1),
					"restart后限流器必须重置（修复前：旧实例已关闭且额度耗尽，checkNow恒false）");
		} finally {
			try {
				lq.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
