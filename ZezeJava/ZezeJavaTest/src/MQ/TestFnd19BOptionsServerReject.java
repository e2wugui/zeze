package MQ;

import java.nio.file.Path;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.MQ.MQManager;
import Zeze.MQ.Master.Master;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FND19 GB-D03 转修回归（Master 端兜底校验）：绕过客户端校验直连 MasterAgent.createMQ 传
 * Raft3/DoubleWrite 时，Master 必须拒绝创建（eOptionsNotImplemented），topic 不落 mqTable。
 * <p>
 * 旧代码：Options 原样存入 mqTable 并回显成功——静默降级为 Single 跑。修复后 ProcessCreateMQRequest
 * 对非 Single（0=未指定除外）返回错误码 9（eOptionsNotImplemented，定义在手写子类），Master 日志
 * 含"未实现"。全程代码构造配置自包含。
 */
public class TestFnd19BOptionsServerReject {
	// 避开 TestMQ 系列（26000-26003）、Reregister（26100/26101）、Resubscribe（26000/26102）、
	// StopLive（26200/26201）。
	private static final int masterPort = 26210;
	private static final int proxyPort = 26211;

	@Test
	public void testMasterRejectsUnimplementedOptions(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();

		var masterHome = tempDir.resolve("mqmaster").toString();
		var master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
		var manager = new MQManager(tempDir.resolve("mqmanager").toString(), managerConfig());
		var agent = new MasterAgent(clientConfig());
		try {
			master.start();
			manager.start();
			agent.startAndWaitConnectionReady();

			// 绕过 MQ.createMQ 客户端校验，直连 MasterAgent 传 Raft3。
			var ex = assertThrows(RuntimeException.class,
					() -> agent.createMQ("topicBOptionsServer", 1, new BOptions.Data(BOptions.Raft3)));
			// 错误码 9（eOptionsNotImplemented）经 IModule.getErrorCode 回传（makeTypeId 低 32 位）。
			assertTrue(ex.getMessage().contains("error=" + Master.eOptionsNotImplemented),
					"Master 必须以 eOptionsNotImplemented 拒绝：message=" + ex.getMessage());

			// 同款拒绝 DoubleWrite。
			assertThrows(RuntimeException.class,
					() -> agent.createMQ("topicBOptionsServer", 1, new BOptions.Data(BOptions.DoubleWrite)));

			// topic 未被创建：openMQ 报 eTopicNotExist（错误码 2），而非旧代码的"成功+回显 Raft3"。
			var openEx = assertThrows(RuntimeException.class, () -> agent.openMQ("topicBOptionsServer"));
			assertTrue(openEx.getMessage().contains("error=2"),
					"被拒绝的 topic 不得落 mqTable：message=" + openEx.getMessage());

			// 未指定（null）形态不受影响：默认 Single 正常创建。
			agent.createMQ("topicBOptionsServerOk", 1, null);
			agent.openMQ("topicBOptionsServerOk");
		} finally {
			agent.stop();
			manager.stop();
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

	private static Config clientConfig() {
		var agentConf = new ServiceConf();
		agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
		var config = new Config();
		config.getServiceConfMap().put(Zeze.MQ.Master.MasterAgent.eServiceName, agentConf);
		return config;
	}
}
