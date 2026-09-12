package UnitTest.Zeze.Component;

import Zeze.Application;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND4-47：jobIdAutoKey 仅在 start() 中赋值，装配顺序不当（Application.start() 调用
 * delayRemove.start() 之前就 addJob）时 NPE 无语义。
 * 修复：入口状态检查抛带语义的 IllegalStateException（对齐timer字段的防御习惯）。
 * @Isolated：避免与其他boot Application的测试并发（全局表注册互斥），独占运行。
 */
@Fast
@Isolated
public class TestDelayRemoveAddJobBeforeStart {

	@Test
	public void testAddJobBeforeStartRejectedExplicitly() throws Exception {
		Task.tryInitThreadPool();
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var app = new Application("TestDelayRemoveBeforeStart", conf);
		try {
			// Application构造即装配DelayRemove（Application.java:227），但delayRemove.start()
			// 在app.start()内才调用——这正是FND4-47的窗口：app未start时addJob。
			Assertions.assertThrows(IllegalStateException.class,
					() -> app.getDelayRemove().addJob("UnitTest.FND4_47", EmptyBean.instance),
					"start前addJob必须显式拒绝（原NPE）");
		} finally {
			// 未start的Application：仅做力所能及的清理，异常忽略。
			try {
				app.stop();
			} catch (Throwable ignored) {
				// ignored
			}
		}
	}
}
