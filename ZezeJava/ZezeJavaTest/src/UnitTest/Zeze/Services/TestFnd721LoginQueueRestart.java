package UnitTest.Zeze.Services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Services.LoginQueue;
import Zeze.Util.DaemonTimer;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;

/**
 * FND7-21：分配tick必须与start/stop配对，stop后restart不得永久失去1秒分配tick与队列
 * 位置广播（排队分配只剩ProcessReportProviderLoad触发的偶发drainQueue）。附带：
 * stop关闭timeThrottle后，provider全部掉线时被重建为limit=0的实例，restart后直到
 * provider重新上报前checkNow恒false，直通分配也被禁。
 * <p>
 * 组件化后（DaemonTimer内聚tick生命周期）：验证restart后分配tick重新处于运行态
 * （isShutdown翻转），timeThrottle重置回首次start状态。tick的真实周期驱动由
 * TestDaemonTimer的组件级红绿覆盖。
 */
@Fast
public class TestFnd721LoginQueueRestart {
	static {
		Task.tryInitThreadPool();
	}

	private static DaemonTimer allocateDaemonOf(LoginQueue lq) throws Exception {
		var field = LoginQueue.class.getDeclaredField("allocateDaemon");
		field.setAccessible(true);
		return (DaemonTimer)field.get(lq);
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
			var daemon = allocateDaemonOf(lq);
			Assertions.assertFalse(daemon.isShutdown(), "start后分配tick必须处于运行态");

			// 排干限流额度：重启前旧实例额度耗尽
			Assertions.assertTrue(timeThrottleOf(lq).checkNow(1), "首次checkNow放行");
			Assertions.assertFalse(timeThrottleOf(lq).checkNow(1), "第二次checkNow被限流（额度耗尽）");

			lq.stop();
			Assertions.assertTrue(daemon.isShutdown(), "stop必须关停分配tick");

			lq.start();
			Assertions.assertFalse(allocateDaemonOf(lq).isShutdown(),
					"restart后分配tick必须恢复运行（修复前：final字段已被cancel，永久失去分配tick与位置广播）");

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
