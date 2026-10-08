package Zeze.MQ;

import java.net.ServerSocket;
import Zeze.Config;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 GB-D04 回归：客户端agent生命周期（引用计数归零停connector重连+复活+强制停机终态）。
 * <p>
 * 修复前：MQ/MQConsumer 全部 close 后静态 agent 的 connector 仍按 1..8 秒退避无限重连
 * （网络错误日志不停、端口/线程资源不释放），MQ.close() 是空壳。
 * <p>
 * 只用本地 agent 实例直测：静态 MQ.mqAgent/MQ.masterAgent 为全进程共享，类级并发下断言其
 * 全局引用数会与其他 @Fast 类（TestMQ 等）竞态（@Fast 准入要求无静态状态竞争）；静态入口
 * （MQ.createMQ/openMQ/MQConsumer 构造的成对 addRef/release、幂等close、close后报错）与
 * 本测试同码路径，由实现+代码审读覆盖。MQ.shutdown() 是进程终态（毒化同 JVM 全部后续 MQ
 * 使用），不在测试中触发；其行为由本地 agent.shutdown() 等价验证。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ（与 TestMQAgentSubscribeRollback 先例一致）。
 */
@Fast
public class TestClientLifecycle {

	@Test
	public void testMQAgentRefCountStopsConnectorReconnect() throws Exception {
		Task.tryInitThreadPool();
		// 本地受端：普通 Service acceptor，接受 MQAgent 连接完成标准握手（重订阅钩子在
		// consumers 空时为 no-op）；端口探测式选取，不与固定端口的并行 @Fast 类冲突。
		int port;
		try (var probe = new ServerSocket(0)) {
			port = probe.getLocalPort();
		}
		var config = new Config();
		var conf = new ServiceConf();
		conf.addAcceptor(new Acceptor(port, null));
		config.getServiceConfMap().put("TestFnd19GBD04Server", conf);
		var server = new Service("TestFnd19GBD04Server", config);
		server.start();
		try {
			var agent = new MQAgent();
			agent.addRef();
			agent.addRef();
			Assertions.assertEquals(2, agent.getRefs());
			var connector = agent.getOrAddConnector("127.0.0.1", port);
			Assertions.assertTrue(waitReady(connector, 10_000), "基线：connector应连上本地受端");
			Assertions.assertFalse(agent.isIdleStopped());

			// 释放一个引用：未归零不得停（在用连接保持）。
			agent.release();
			Assertions.assertEquals(1, agent.getRefs());
			Assertions.assertFalse(agent.isIdleStopped(), "引用未归零不得停connector");
			Assertions.assertNotNull(connector.TryGetReadySocket(), "在用连接保持");

			// 归零：停connector重连+关socket（不再退避续排）。
			agent.release();
			Assertions.assertEquals(0, agent.getRefs());
			Assertions.assertTrue(agent.isIdleStopped(), "引用归零停connector");
			Assertions.assertNull(connector.getSocket(), "socket已关闭");
			Assertions.assertFalse(connector.isConnected());

			// 无重连残留（形态断言）：受端仍在监听，若重连引擎未停，初始约1秒的退避重连
			// 必已重建连接——等待超过一个退避周期后仍无连接即证重连引擎已停。
			Thread.sleep(2_500);
			Assertions.assertNull(connector.TryGetReadySocket(), "归零停机后不得重连（无重连残留）");

			// 复活：新引用到达，getOrAddConnector 重启存量connector（幂等start）。
			agent.addRef();
			Assertions.assertFalse(agent.isIdleStopped(), "新引用复活");
			agent.getOrAddConnector("127.0.0.1", port);
			Assertions.assertTrue(waitReady(connector, 10_000), "复活后connector重启重连");

			// 强制停机（MQ.shutdown()的agent侧形态）：终态，此后addRef明确报错；幂等。
			agent.release();
			Assertions.assertTrue(agent.isIdleStopped());
			agent.shutdown();
			Assertions.assertTrue(agent.isTerminated());
			Assertions.assertNull(connector.getSocket(), "强制停机关socket");
			Assertions.assertThrows(IllegalStateException.class, agent::addRef,
					"shutdown后取引用必须明确报错（不是无声空转）");
			agent.shutdown(); // 幂等
		} finally {
			server.stop();
		}
	}

	@Test
	public void testMasterAgentRefCountAndShutdown() {
		var agent = new MasterAgent(new Config()); // 空配置无connector：纯引用计数形态断言
		agent.addRef();
		agent.addRef();
		agent.release();
		Assertions.assertEquals(1, agent.getRefs());
		Assertions.assertFalse(agent.isIdleStopped());
		agent.release();
		Assertions.assertEquals(0, agent.getRefs());
		Assertions.assertTrue(agent.isIdleStopped(), "归零停connector");
		agent.addRef(); // 复活
		Assertions.assertFalse(agent.isIdleStopped());
		agent.release();
		agent.shutdown();
		Assertions.assertTrue(agent.isTerminated());
		Assertions.assertThrows(IllegalStateException.class, agent::addRef, "shutdown后取引用必须明确报错");
	}

	private static boolean waitReady(Connector connector, long timeoutMs) throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (connector.TryGetReadySocket() != null)
				return true;
			Thread.sleep(50);
		}
		return false;
	}
}
