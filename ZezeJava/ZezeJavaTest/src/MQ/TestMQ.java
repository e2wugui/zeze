package MQ;

import harness.Fast;
import java.nio.file.Path;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.MQ.MQ;
import Zeze.MQ.MQConsumer;
import Zeze.MQ.MQManager;
import Zeze.MQ.MQProducer;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestMQ {
	// MQ 客户端（MQProducer/MQConsumer 内的静态 agent）仍从默认配置 zeze.xml 读取
	// Zeze.MQ.Master.Agent 的 Connector，master 端口必须与其保持一致。
	private static final int masterPort = 26000;

	// 配置全部代码构造，不依赖 mq*.xml；master 在 home 里持久化各分区的绝对 host:port，
	// 换端口/IP 的旧数据会指向死端口，一次性临时 home 保证每次运行自包含、可重复。
	@Test
	public void testMQ(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();

		var master = new Zeze.MQ.Master.Main(tempDir.resolve("mqmaster").toString(), masterConfig());
		var manager0 = new MQManager(tempDir.resolve("mqmanager0").toString(), managerConfig(26001));
		var manager1 = new MQManager(tempDir.resolve("mqmanager1").toString(), managerConfig(26002));
		var manager2 = new MQManager(tempDir.resolve("mqmanager2").toString(), managerConfig(26003));
		MQProducer producer = null;
		MQConsumer consumer = null;
		try {
			master.start();
			manager0.start();
			manager1.start();
			manager2.start();
			var topic = "topicTest";
			try {
				MQ.createMQ(topic, 6, new BOptions.Data(BOptions.Single));
			} catch (Exception ex) {
				// skip
			}
			producer = new MQProducer(topic);
			producer.sendMessage(new BMessage.Data());

			consumer = new MQConsumer(topic, pushMessage -> System.out.println("consumer " + pushMessage.getTopic()));
		} finally {
			if (producer != null)
				producer.close();
			if (consumer != null)
				consumer.close();
			manager0.stop();
			manager1.stop();
			manager2.stop();
			master.stop();
		}
	}

	private static Config masterConfig() {
		var masterConf = new ServiceConf();
		masterConf.getSocketOptions().setInputBufferMaxProtocolSize(2 * 1024 * 1024);
		masterConf.addAcceptor(new Acceptor(masterPort, null));
		var config = new Config();
		config.getServiceConfMap().put("Zeze.MQ.Master", masterConf);
		return config;
	}

	private static Config managerConfig(int proxyPort) {
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
