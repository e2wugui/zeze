package Zeze.MQ;

import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;

/**
 * FND19 MQ 网络测试共享拓扑（对齐 Dbh2/Fnd19GADStubSupport 先例）：Master + Manager + 客户端
 * 三进程形态的 ServiceConf 工厂。拓扑契约一份——Master acceptor 2MB 协议上限；Manager 双
 * ServiceConf（连 Master 的 MasterAgent connector + 暴露给消费者的 proxy acceptor）；客户端仅
 * MasterAgent connector。
 * <p>
 * 端口注册表（固定端口，新网络测试在用段之外选取并回填此处）：TestMQ 26000-26003、
 * TestMQConsumerResubscribe 26102（master 复用 26000）、TestMQManagerReregister 26100/26101、
 * TestMQManagerStopLive 26200/26201、TestBOptionsServerReject 26210/26211、
 * TestGBD05RouteRewrite 26220/26221/26222、TestMQManagerRemintKeepsPartitions 26231
 * （master 复用 26000）、TestTopicNameFsAliasRejected 26240/26241。
 */
final class Fnd19MqNetTestSupport {
	private Fnd19MqNetTestSupport() {
	}

	static Config masterConfig(int masterPort) {
		var masterConf = new ServiceConf();
		masterConf.getSocketOptions().setInputBufferMaxProtocolSize(2 * 1024 * 1024);
		masterConf.addAcceptor(new Acceptor(masterPort, null));
		var config = new Config();
		config.getServiceConfMap().put("Zeze.MQ.Master", masterConf);
		return config;
	}

	static Config managerConfig(int masterPort, int proxyPort) {
		var agentConf = new ServiceConf();
		agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
		var proxyConf = new ServiceConf();
		proxyConf.addAcceptor(new Acceptor(proxyPort, "127.0.0.1"));
		var config = new Config();
		config.getServiceConfMap().put(Zeze.MQ.Master.MasterAgent.eServiceName, agentConf);
		config.getServiceConfMap().put(ProxyServer.eProxyServerName, proxyConf);
		return config;
	}

	static Config clientConfig(int masterPort) {
		var agentConf = new ServiceConf();
		agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
		var config = new Config();
		config.getServiceConfMap().put(Zeze.MQ.Master.MasterAgent.eServiceName, agentConf);
		return config;
	}
}
