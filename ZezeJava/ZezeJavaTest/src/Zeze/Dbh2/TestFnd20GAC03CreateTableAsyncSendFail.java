package Zeze.Dbh2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND20 GA-C03回归：MasterAgent.createTableAsync必须处理Send返回false。
 * master连接空窗期（GetSocket()==null）下Rpc.Send不发送、不建上下文、不派发回调，
 * bug时返回值被丢弃：回调永不触发→Dbh2Table.ready永不完成→waitReady永久挂起，
 * 且完全旁路GA-D04的预算（预算只在回调内结算）。修复=Send失败显式回调
 * eTooFewManager（资源暂时不足类码，走白名单重试，重连后照常建表）。
 */
@Fast
public class TestFnd20GAC03CreateTableAsyncSendFail {

	@Test
	public void testSendFailInvokesCallback() throws Exception {
		Task.tryInitThreadPool();
		// 不start：无任何连接，GetSocket()==null，Send必返回false。
		var agent = new MasterAgent(new Config());
		var called = new CountDownLatch(1);
		var rcBox = new AtomicInteger(Integer.MIN_VALUE);
		agent.createTableAsync("db1", "t1", (rc, isNew, table) -> {
			rcBox.set(rc);
			called.countDown();
		});
		Assertions.assertTrue(called.await(2, TimeUnit.SECONDS),
				"Send失败（无master连接）必须显式回调失败码——bug时回调永不触发，waitReady永久挂起");
		Assertions.assertEquals(MasterAgent.eTooFewManager, rcBox.get(),
				"失败码必须是白名单内的暂时性码（走createTableWithRetry重试+预算）");
	}
}
