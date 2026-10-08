package Zeze.MQ;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manager 换址迁移（同 home 同 managerId、新 proxy 地址重注册，Master rewriteRoutes 路由
 * 自愈）后既存消费者的路由跟随回归：消费者构造时把 openMQ 解析出的地址集固化为
 * connector 集（getManagers 自述"构造时确定，不再变更"），重订阅链
 * （OnHandshakeDone→reSubscribeRound）只遍历既有连接器——迁移后旧地址死亡，消费者
 * 持旧连接器无限重连旧址、reSubscribeRound 永无触发点；新 Manager 的订阅表为空
 * （subscribes 纯内存态），分区 bind(0,null)、tryPushMessage 静默短路，消息在新
 * Manager 上无限积压，应用侧零信号（MQListener 只收消息不收错误）——Master 侧的
 * "路由自愈"只覆盖路由表与新建客户端，对既存消费者是半截工程。
 * <p>
 * 修复（拉式对账，零协议改动）：MQAgent 周期路由对账——对每个存活消费者重取一次
 * Master 路由，地址集差量补建 connector 并入 managers（只增不减）；新连接器握手完成
 * 后由既有 OnHandshakeDone→reSubscribeRound 链以原 sessionId 幂等补发 Subscribe，
 * 恢复推送。测试经反射把对账周期缩到 500ms（baseline 无该缝则按原样超时判红）。
 * <p>
 * 客户端（MQConsumer 内的静态 agent）从默认 zeze.xml 读 master 26000，@Isolated
 * 独占；含迁移等待与重连轮询，不标 @Fast（integrationTest）。
 */
@Isolated
public class TestMQConsumerRouteRefresh {
	private static final int masterPort = 26000;
	// 避开注册表已用段（TestMQ 26001-26003、TestMQConsumerResubscribe 26102、
	// TestRouteRewrite 26221/26222、TestTopicNameSelfFsAliasRejected 26242/26243）。
	private static final int proxyPortOld = 26244;
	private static final int proxyPortNew = 26245;

	@Test
	public void testConsumerFollowsManagerAddressMigration(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var originalPeriod = trySetRouteRefreshPeriod(500); // baseline 无缝=原样（判红路径）

		var masterHome = tempDir.resolve("mqmaster").toString();
		var managerHome = tempDir.resolve("mqmanager").toString();
		var topic = "topicConsumerRouteRefresh";
		var master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
		var manager = new MQManager(managerHome, managerConfig(proxyPortOld));
		var agent = new MasterAgent(MqNetTestSupport.clientConfig(masterPort));
		MQConsumer consumer = null;
		MQProducer producer = null;
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();

			MQ.createMQ(topic, 1, new BOptions.Data(BOptions.Single));
			assertEquals(proxyPortOld, agent.openMQ(topic).getServers().get(0).getPort(),
					"前置：初始路由指向旧址");

			var received = new CountDownLatch(1);
			consumer = new MQConsumer(topic, m -> received.countDown());
			var sessionId = consumer.getSessionId();
			assertTrue(waitSubscribed(manager, topic, sessionId, 10_000),
					"baseline subscribe not visible on old-address manager");

			// 换址迁移：同 home（同 managerId）新 proxy 地址重启 Manager——register 联动
			// rewriteRoutes 把路由自愈到新地址（GBD05 语义）。旧地址死亡，消费者进程不重启。
			var oldManagerId = manager.getManagerId();
			manager.stop();
			manager = null; // finally 只停未停实例（stop 非幂等语义未约定）
			var manager2 = new MQManager(managerHome, managerConfig(proxyPortNew));
			try {
				manager2.start();
				assertEquals(oldManagerId, manager2.getManagerId(), "同 home 重启身份不变");

				// 路由自愈到达（重连退避量级，轮询等待）。
				var deadline = System.currentTimeMillis() + 30_000;
				int routedPort = -1;
				while (System.currentTimeMillis() < deadline) {
					routedPort = agent.openMQ(topic).getServers().get(0).getPort();
					if (routedPort == proxyPortNew)
						break;
					Thread.sleep(200);
				}
				assertEquals(proxyPortNew, routedPort, "迁移后 Master 路由必须已自愈到新地址（前置）");

				// 核心断言（修复前红）：既存消费者必须最终以原 sessionId 重订到新地址
				// Manager——修复前 managers 连接集构造时固化，重订永远打旧址（20s 轮询超时）。
				assertTrue(waitSubscribed(manager2, topic, sessionId, 20_000),
						"consumer not re-subscribed to new-address manager after migration"
								+ "（路由快照不刷新：旧连接器无限重连旧址，迁移分区静默饿死）");

				// 端到端：迁移后新建生产者（新路由）发送必须送达既存消费者（修复前只落盘不推送）。
				producer = new MQProducer(topic);
				producer.sendMessage(new BMessage.Data());
				assertTrue(received.await(30, TimeUnit.SECONDS),
						"push message not received after manager address migration");

				producer.close();
				producer = null;
				consumer.close();
				consumer = null;
			} finally {
				manager2.stop();
			}
		} finally {
			if (producer != null)
				producer.close();
			if (consumer != null)
				consumer.close();
			agent.stop();
			if (null != manager)
				manager.stop();
			master.stop();
			if (originalPeriod >= 0)
				trySetRouteRefreshPeriod(originalPeriod);
		}
	}

	/** 反射缩短 MQAgent 路由对账周期并作废在飞排期（同 JVM 前序测试类按默认 30s 周期排的
	 * 轮次会跨进本测试窗口，致迁移跟随延迟超断言预算）；作废后由本测试消费者的订阅按新
	 * 周期重新武装。baseline 无该缝返回 -1（判红路径原样走超时）。 */
	private static long trySetRouteRefreshPeriod(long periodMs) {
		try {
			var agent = MQ.mqAgent;
			Field f = MQAgent.class.getDeclaredField("routeRefreshPeriodMs");
			f.setAccessible(true);
			var original = f.getLong(agent);
			f.setLong(agent, periodMs);
			Field pending = MQAgent.class.getDeclaredField("routeRefreshFuture");
			pending.setAccessible(true);
			var future = (java.util.concurrent.Future<?>)pending.get(agent);
			if (null != future)
				future.cancel(false); // 已取消的句柄 isDone=true：armRouteRefresh 即重排新周期
			return original;
		} catch (ReflectiveOperationException e) {
			return -1; // baseline：机制不存在，走超时判红
		}
	}

	private static boolean waitSubscribed(MQManager manager, String topic, long sessionId, long timeoutMs)
			throws Exception {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			var partition = manager.getQueueForTest(topic);
			if (null != partition && partitionSubscribes(partition).containsKey(sessionId))
				return true;
			Thread.sleep(200);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, AsyncSocket> partitionSubscribes(MQPartition partition) throws Exception {
		var subsField = MQPartition.class.getDeclaredField("subscribes");
		subsField.setAccessible(true);
		return (ConcurrentHashMap<Long, AsyncSocket>)subsField.get(partition);
	}

	private static Zeze.Config masterConfig() {
		var masterConf = new ServiceConf();
		masterConf.getSocketOptions().setInputBufferMaxProtocolSize(2 * 1024 * 1024);
		masterConf.addAcceptor(new Acceptor(masterPort, null));
		var config = new Zeze.Config();
		config.getServiceConfMap().put("Zeze.MQ.Master", masterConf);
		return config;
	}

	private static Zeze.Config managerConfig(int proxyPort) {
		var agentConf = new ServiceConf();
		agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
		var proxyConf = new ServiceConf();
		proxyConf.addAcceptor(new Acceptor(proxyPort, "127.0.0.1"));
		var config = new Zeze.Config();
		config.getServiceConfMap().put(Zeze.MQ.Master.MasterAgent.eServiceName, agentConf);
		config.getServiceConfMap().put(ProxyServer.eProxyServerName, proxyConf);
		return config;
	}
}
