package Zeze.MQ;

import java.nio.file.Path;
import Zeze.MQ.MQManager;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GB-D05 回归（拍板方案A：managerId 持久身份 + Register 联动重写 mqTable）。
 * <p>
 * 修复前：topic→manager(host:port) 的映射只在 CreateMQ 时写入一次；Manager 换地址重注册后
 * mqTable 仍是死地址，openMQ 回放旧地址、该分区生产消费双失效（只能人工改 rocksdb）。
 * <p>
 * 修复后：Register 携带 Manager 稳定身份（home/.managerId 自铸持久化），Master 注册时对 mqTable
 * 中该 id 承载的 servers 条目联动重写新地址——同一 home 换端口重启（=换地址重注册）后，
 * openMQ 解析出的路由更新为新地址（路由自愈），managerId 保持不变。
 * <p>
 * Master 拓扑见 Fnd19MqNetTestSupport；含 Manager 重启等待，不标 @Fast（integrationTest）。
 */
public class TestGBD05RouteRewrite {
	private static final int masterPort = 26220;
	private static final int proxyPort1 = 26221;
	private static final int proxyPort2 = 26222;

	@Test
	public void testReregisterWithNewAddressRewritesRoute(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();

		var masterHome = tempDir.resolve("mqmaster").toString();
		var managerHome = tempDir.resolve("mqmanager").toString();
		var master = new Zeze.MQ.Master.Main(masterHome, Fnd19MqNetTestSupport.masterConfig(masterPort));
		var manager = new MQManager(managerHome, Fnd19MqNetTestSupport.managerConfig(masterPort, proxyPort1));
		var agent = new MasterAgent(Fnd19MqNetTestSupport.clientConfig(masterPort));
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();

			var managerId1 = manager.getManagerId();
			assertTrue(managerId1 > 0, "managerId 必须为正（negativeCheck 约束）");
			assertTrue(java.nio.file.Files.exists(Path.of(managerHome, ".managerId")),
					"managerId 必须持久化于 home/.managerId");

			agent.createMQ("topicD05", 1, null);
			var servers1 = agent.openMQ("topicD05").getServers();
			assertEquals(1, servers1.size());
			assertEquals(proxyPort1, servers1.get(0).getPort(), "初始路由指向注册地址");
			assertEquals(managerId1, servers1.get(0).getManagerId(), "创建路径写入的 BMQServer 带上 ManagerId");

			// 换地址重注册：同一 home（=同一 managerId）换 proxy 端口重启 Manager。
			manager.stop();
			manager = null; // finally 只停未停实例（stop 非幂等语义未约定）
				var manager2 = new MQManager(managerHome, Fnd19MqNetTestSupport.managerConfig(masterPort, proxyPort2));
			try {
				manager2.start(); // register 携带同 managerId + 新地址 → Master 联动重写 mqTable
				assertEquals(managerId1, manager2.getManagerId(), "同 home 重启身份不变（.managerId 持久化）");

				// 轮询等待重注册到达（重连退避 1..8s 量级）；修复前 mqTable 永远是旧地址，此断言超时失败。
				var deadline = System.currentTimeMillis() + 30_000;
				int routedPort = -1;
				long routedManagerId = -1;
				while (System.currentTimeMillis() < deadline) {
					var servers = agent.openMQ("topicD05").getServers();
					routedPort = servers.get(0).getPort();
					routedManagerId = servers.get(0).getManagerId();
					if (routedPort == proxyPort2)
						break;
					Thread.sleep(200);
				}
				assertEquals(proxyPort2, routedPort, "换地址重注册后路由必须更新为新地址（自愈）");
				assertEquals(managerId1, routedManagerId, "路由条目身份保持");
			} finally {
				manager2.stop();
			}
		} finally {
			agent.stop();
			if (null != manager)
				manager.stop();
			master.stop();
		}
	}
}
