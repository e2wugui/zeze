package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-04回归：停机窗口内GlobalAgentBase.setActiveTime解引用已置空的
 * AchillesHeelDaemon。Application.stop先stopAndJoin并置null daemon、之后才globalAgent.stop，
 * 窗口内组件事务acquire成功路径与IO线程Login/KeepAlive回调仍会调setActiveTime——
 * 修复前对null解引用，NPE打进事务/IO线程（本可提交的事务假性失败）。
 * 测试：daemon缺席（未启动的Application）时setActiveTime不得抛NPE（刷新无意义即跳过）。
 */
@Fast
public class TestGlobalAgentSetActiveTimeNoDaemon {

	@Test
	public void testSetActiveTimeWithoutDaemonDoesNotThrow() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		var app = new Application("TestT04NoDaemon", conf);
		Assertions.assertNull(app.getAchillesHeelDaemon(), "未启动的Application不得有daemon");

		var agentBase = new GlobalAgentBase(app) {
			@Override
			protected void cancelPending() {
			}

			@Override
			public void keepAlive() {
			}
		};

		// 核心（红）：daemon为null时不得NPE（修复前直接解引用抛NPE）
		Assertions.assertDoesNotThrow(() -> agentBase.setActiveTime(System.currentTimeMillis()));
		Assertions.assertEquals(agentBase.getActiveTime(), agentBase.getActiveTime());
	}
}
