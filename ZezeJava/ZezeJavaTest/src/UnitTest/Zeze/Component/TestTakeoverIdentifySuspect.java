package UnitTest.Zeze.Component;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManagerServer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 步骤②提示通道回归：Agent连接时上报Identify（SM把serverId记在会话上），
 * 断线时SM立即向其余会话广播Suspect（不再走旧的600s延迟通知）。
 * 进程内SM（动态空闲端口+独立autokeys目录），两个Agent（serverId=11/12）。
 */
@Fast
public class TestTakeoverIdentifySuspect {

	@Test
	public void testSuspectBroadcastOnClose() throws Exception {
		// 动态分配空闲端口（@Fast禁固定端口；端口0的绑定结果无法从SM取出，用预探测）。
		// 必须TCP+UDP双探测：SM的Id128UdpServer要在同端口号绑UDP，全量单JVM运行时
		// 残留的Id128Udp实例可能占着该UDP端口（TCP空闲≠UDP空闲，实测BindException）。
		int port = -1;
		for (int attempt = 0; attempt < 32 && port < 0; ++attempt) {
			int candidate;
			try (var ss = new ServerSocket(0)) {
				candidate = ss.getLocalPort();
			}
			try (var ds = new java.net.DatagramSocket(candidate)) {
				port = candidate; // TCP+UDP都空闲才使用
			} catch (java.net.BindException ignore) {
				// UDP被占（如残留Id128UdpClient的临时端口），换下一个
			}
		}
		Assertions.assertTrue(port > 0, "32次尝试内未找到TCP+UDP同时空闲的端口");
		// autokeys目录放在已被gitignore的autokeys/下，避免污染仓库；RocksDB需要父目录存在。
		Files.createDirectories(Path.of("autokeys"));

		var sm = new ServiceManagerServer(null, port, new Config(), "autokeys/takeover-e2e");
		Agent agent1 = null;
		Agent agent2 = null;
		try {
			agent1 = newAgent(port, 11);
			agent2 = newAgent(port, 12);
			agent1.start();
			agent2.start();
			agent1.waitReady();
			agent2.waitReady();

			var suspected = new LinkedBlockingQueue<Integer>();
			agent2.setOnSuspect(suspected::add);

			// 等Identify在SM侧生效（onConnected异步发送；负载下未处理即断线会话上无serverId，
			// SM不广播Suspect，用例flaky——全量单JVM运行实证过）。
			Thread.sleep(1000);

			// agent1正常关闭：连接断开→SM onClose→Suspect(serverId=11)广播→agent2回调。
			agent1.stop();
			agent1 = null;

			var got = suspected.poll(5, TimeUnit.SECONDS);
			Assertions.assertNotNull(got, "断线后应收到Suspect广播");
			Assertions.assertEquals(11, got, "Suspect应携带Identify上报的serverId");
		} finally {
			if (agent1 != null)
				agent1.stop();
			if (agent2 != null)
				agent2.stop();
			sm.close();
		}
	}

	private static Agent newAgent(int port, int serverId) throws Exception {
		var conf = new Config();
		conf.setServerId(serverId);
		var agent = new Agent(conf);
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", port));
		return agent;
	}
}
