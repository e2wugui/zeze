package MQ;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.MQ.MQ;
import Zeze.MQ.MQConsumer;
import Zeze.MQ.MQManager;
import Zeze.MQ.MQPartition;
import Zeze.MQ.MQProducer;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manager 重启（同 home）后消费者必须在连接自动重连时重发 Subscribe（FND2-G2-2）：
 * subscribes 是 Manager 纯内存态，重启即清空；修复前 MQAgent.Service 无任何重连钩子，
 * MQConsumer 仅构造时订阅一次——重连成功后既有消费者连接健康却永不重订阅，分区
 * bindSocket==null、消息持续落盘，消费者静默饿死直到自身进程重启。
 * <p>
 * 客户端（MQConsumer 内的静态 agent）从默认 zeze.xml 读取 Zeze.MQ.Master.Agent 的 Connector，
 * master 端口必须保持 26000。
 * <p>
 * 含消费者断线重连（指数退避 1..8 秒）加轮询等待，不标 @Fast（integrationTest）。
 */
@Isolated // master 端口 26000 与 TestMQ 系列相同（MQConsumer 静态 agent 读默认 zeze.xml），类级并发下会端口冲突，独占运行
public class TestMQConsumerResubscribe {
	private static final int masterPort = 26000;
	// 避开 TestMQ 三 manager 的 26001-26003 与 TestMQManagerReregister 的 26101。
	private static final int proxyPort = 26102;

	@Test
	public void testResubscribeAfterManagerRestart(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();

		var masterHome = tempDir.resolve("mqmaster").toString();
		var managerHome = tempDir.resolve("mqmanager").toString();
		var master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
		var manager = new MQManager(managerHome, managerConfig());
		var topic = "topicConsumerResubscribe";
		MQConsumer consumer = null;
		MQProducer producer = null;
		try {
			master.start();
			manager.start();
			try {
				MQ.createMQ(topic, 1, new BOptions.Data(BOptions.Single));
			} catch (Exception ex) {
				// skip
			}

			var received = new CountDownLatch(1);
			consumer = new MQConsumer(topic, m -> received.countDown());
			var sessionId = consumer.getSessionId();
			assertTrue(waitSubscribed(manager, topic, sessionId, 10_000),
					"baseline subscribe not visible on manager");

			// 模拟 Manager 崩溃重启（同 home）：subscribes 纯内存态被清空；
			// 消费者进程不重启，依赖连接自动重连+重发 Subscribe 恢复。
			manager.stop();
			manager = new MQManager(managerHome, managerConfig());
			manager.start();

			// 修复前：重连成功但 Subscribe 永不重发，subscribes 恒空——轮询超时断言失败。
			assertTrue(waitSubscribed(manager, topic, sessionId, 30_000),
					"consumer not re-subscribed after manager restart");

			// 端到端：重启后发送的消息必须送达（修复前只落盘不推送，await 超时）。
			producer = new MQProducer(topic);
			producer.sendMessage(new BMessage.Data());
			assertTrue(received.await(30, TimeUnit.SECONDS),
					"push message not received after manager restart");

			producer.close();
			producer = null;
			consumer.close();
			consumer = null;
		} finally {
			if (producer != null)
				producer.close();
			if (consumer != null)
				consumer.close();
			manager.stop();
			master.stop();
		}
	}

	private static boolean waitSubscribed(MQManager manager, String topic, long sessionId, long timeoutMs)
			throws Exception {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (partitionSubscribes(manager, topic).containsKey(sessionId))
				return true;
			Thread.sleep(500);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<Long, AsyncSocket> partitionSubscribes(MQManager manager, String topic)
			throws Exception {
		var queuesField = MQManager.class.getDeclaredField("queues");
		queuesField.setAccessible(true);
		var queues = (ConcurrentHashMap<String, MQPartition>)queuesField.get(manager);
		var subsField = MQPartition.class.getDeclaredField("subscribes");
		subsField.setAccessible(true);
		return (ConcurrentHashMap<Long, AsyncSocket>)subsField.get(queues.get(topic));
	}

	private static Config masterConfig() {
		var masterConf = new ServiceConf();
		masterConf.getSocketOptions().setInputBufferMaxProtocolSize(2 * 1024 * 1024);
		masterConf.addAcceptor(new Acceptor(masterPort, null));
		var config = new Config();
		config.getServiceConfMap().put("Zeze.MQ.Master", masterConf);
		return config;
	}

	private static Config managerConfig() {
		var agentConf = new ServiceConf();
		agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
		var proxyConf = new ServiceConf();
		proxyConf.addAcceptor(new Acceptor(proxyPort, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put(Zeze.MQ.Master.MasterAgent.eServiceName, agentConf);
		config.getServiceConfMap().put(ProxyServer.eProxyServerName, proxyConf);
		return config;
	}
}
