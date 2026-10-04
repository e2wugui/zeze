package Zeze.MQ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Builtin.MQ.Master.BReportPartitions;
import Zeze.Builtin.MQ.Master.BTopicPartitions;
import Zeze.Config;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * managerId 重铸（home/.managerId 丢失）后同 home 数据的存活端到端回归：Manager 换代重注册、
 * 周期上报触发 Master 孤儿对账，旧 id 条目不得判孤儿下发 DeletePartition——上报本身即数据
 * 延续证据，路由证据化转移为上报者；分区数据保留，收发照常。
 * <p>
 * 修复前：旧 id 条目对换代 Manager 恒"未覆盖"，宽限期满 Master 自动下发 DeletePartition，
 * Manager 无条件执行存储全清——单文件丢失事件放大为整 Manager 数据灭失。
 * <p>
 * Master 宽限期经 MQConfig 定制段收缩为 0（对账判定与默认 10 分钟同构）；客户端
 * （MQ/MQProducer/MQConsumer 内的静态 agent）从默认 zeze.xml 读取 26000 连 master。
 * 含 Manager 重启与对账等待，不标 @Fast（integrationTest）。
 */
@ResourceLock("mq-file-statics") // 旁观者READ：与改写trunkFileSize/makeIndexPeriod的类互斥——静态被并行改小期间本类append会滚出无索引段，fillMessage seekForPrev落空即messageIndexNotFound假红（2026-10-04 test40批r5实证）；旁观者彼此READ可并行
@Isolated // master 端口 26000 与 TestMQ 系列相同（MQConsumer 静态 agent 读默认 zeze.xml），独占运行
public class TestMQManagerRemintKeepsPartitions {
	private static final int masterPort = 26000;
	// 避开 TestMQ 26001-26003、TestMQManagerReregister 26101、TestMQConsumerResubscribe 26102、
	// TestGBD05RouteRewrite 26221/26222。
	private static final int proxyPort = 26231;

	@Test
	public void testRemintedManagerKeepsPartitionsAfterReconcile(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();

		var masterHome = tempDir.resolve("mqmaster").toString();
		var managerHome = tempDir.resolve("mqmanager").toString();
		var topic = "topicRemintKeeps";
		var master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
		var manager = new MQManager(managerHome, managerConfig());
		var agent = new MasterAgent(Fnd19MqNetTestSupport.clientConfig(masterPort));
		MQProducer producer = null;
		MQConsumer consumer = null;
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();
			var oldManagerId = manager.getManagerId();

			// 造数据：1 条已持久化消息（无消费者，留作跨换代存活的证据）。
			MQ.createMQ(topic, 1, new BOptions.Data(BOptions.Single));
			producer = new MQProducer(topic);
			producer.sendMessage(new BMessage.Data());
			var partition = manager.getQueueForTest(topic).get(0);
			assertNotNull(partition, "baseline partition must exist");
			assertEquals(1, partition.getFileForTest().getNextMessageId(), "baseline message must be persisted");

			// 换代：.managerId 丢失（备份遗漏 dotfile/截断为空的运维形态），同 home 重启重铸新 id。
			manager.stop();
			Files.delete(Path.of(managerHome, ".managerId"));
			manager = new MQManager(managerHome, managerConfig());
			manager.start();
			assertNotEquals(oldManagerId, manager.getManagerId(), "id 文件丢失后必须重铸新身份");

			// 驱动上报对账（等价 loadMonitorTimer 周期上报到达 Master）：
			// 修复前——旧 id 条目判孤儿，宽限 0 立即下发 DeletePartition，分区存储全清；
			// 修复后——旧 id 属主无存活连接，上报即数据延续证据，不删并转移路由。
			var report = new BReportPartitions.Data();
			var tp = new BTopicPartitions.Data();
			tp.setTopic(topic);
			tp.getPartitionIndexes().add(0);
			report.getTopics().add(tp);
			manager.getMasterAgent().reportPartitions(report);

			// 路由证据化转移落地（对账在应答之后执行，轮询等待）。
			var deadline = System.currentTimeMillis() + 10_000;
			long routedManagerId = -1;
			while (System.currentTimeMillis() < deadline) {
				routedManagerId = agent.openMQ(topic).getServers().get(0).getManagerId();
				if (routedManagerId == manager.getManagerId())
					break;
				Thread.sleep(200);
			}
			assertEquals(manager.getManagerId(), routedManagerId, "旧 id 条目须证据化转移为上报者 id");

			// 数据存活：分区仍在、位点未回退（修复前此刻已被 DeletePartition 清空）。
			var survived = manager.getQueueForTest(topic).get(0);
			assertNotNull(survived, "分区不得被孤儿对账删除");
			assertEquals(1, survived.getFileForTest().getNextMessageId(), "已持久化消息必须存活");

			// 收发照常：订阅送达换代前的积压消息 + 新发送的消息。
			var received = new CountDownLatch(2);
			consumer = new MQConsumer(topic, m -> received.countDown());
			assertTrue(waitSubscribed(manager, topic, consumer.getSessionId(), 10_000),
					"consumer not subscribed after manager remint");
			producer.sendMessage(new BMessage.Data());
			assertTrue(received.await(30, TimeUnit.SECONDS),
					"backlog + new message must both arrive after manager remint");
		} finally {
			if (producer != null)
				producer.close();
			if (consumer != null)
				consumer.close();
			agent.stop();
			manager.stop();
			master.stop();
		}
	}

	private static boolean waitSubscribed(MQManager manager, String topic, long sessionId, long timeoutMs)
			throws Exception {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			var queue = manager.getQueueForTest(topic);
			if (null != queue && partitionSubscribes(queue).containsKey(sessionId))
				return true;
			Thread.sleep(200);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, AsyncSocket> partitionSubscribes(MQPartition queue) throws Exception {
		var subsField = MQPartition.class.getDeclaredField("subscribes");
		subsField.setAccessible(true);
		return (ConcurrentHashMap<Long, AsyncSocket>)subsField.get(queue);
	}

	private static Config masterConfig() throws Exception {
		var masterConf = new ServiceConf();
		masterConf.getSocketOptions().setInputBufferMaxProtocolSize(2 * 1024 * 1024);
		masterConf.addAcceptor(new Acceptor(masterPort, null));
		var config = new Config();
		config.getServiceConfMap().put("Zeze.MQ.Master", masterConf);
		// 定制段注入：OrphanGracePeriodMs=0（候选即满龄，判定逻辑与默认 10 分钟同构）。
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var mqConfElem = doc.createElement("MQConfig");
		mqConfElem.setAttribute("OrphanGracePeriodMs", "0");
		config.getCustomizes().put("MQConfig", mqConfElem);
		return config;
	}

	private static Config managerConfig() {
		var agentConf = new ServiceConf();
		agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
		var proxyConf = new ServiceConf();
		proxyConf.addAcceptor(new Acceptor(proxyPort, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put(MasterAgent.eServiceName, agentConf);
		config.getServiceConfMap().put(ProxyServer.eProxyServerName, proxyConf);
		return config;
	}
}
