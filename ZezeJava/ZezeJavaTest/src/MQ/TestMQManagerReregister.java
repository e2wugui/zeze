package MQ;

import java.nio.file.Path;
import Zeze.Config;
import Zeze.MQ.MQManager;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Master 重启后 Manager 必须在重连时重发 Register，否则新 Master 的 managers 永久为空，
 * createMQ 永久 eManagerNotFound（FND-G2-5）。全程代码构造配置自包含。
 * <p>
 * 等待 Manager 断线重连（指数退避 1..8 秒）加轮询恢复，含秒级等待，不标 @Fast（integrationTest）。
 */
public class TestMQManagerReregister {
    // 避开 TestMQ 系列占用的 26000-26003。
    private static final int masterPort = 26100;

    @Test
    public void testReregisterAfterMasterRestart(@TempDir Path tempDir) throws Exception {
        Task.tryInitThreadPool();

        var masterHome = tempDir.resolve("mqmaster").toString();
        var master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
        var manager = new MQManager(tempDir.resolve("mqmanager").toString(), managerConfig(26101));
        var agent = new MasterAgent(clientConfig());
        try {
            master.start();
            manager.start();
            agent.startAndWaitConnectionReady();

            agent.createMQ("t1", 1, null); // 重启前基线：createMQ 正常

            // 模拟 Master 崩溃重启（同 home）；Manager 进程不重启，依赖自动重连+重注册恢复。
            master.stop();
            master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
            master.start();

            // 修复前：Manager 重连成功但不再注册，createMQ 永久 eManagerNotFound。
            assertTrue(waitCreateMQ(agent, "t2"),
                    "createMQ not recovered after master restart (manager re-register failed)");
        } finally {
            agent.stop();
            manager.stop();
            master.stop();
        }
    }

    private static boolean waitCreateMQ(MasterAgent agent, String topic) throws InterruptedException {
        var deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                agent.createMQ(topic, 1, null);
                return true;
            } catch (Exception ex) {
                Thread.sleep(500);
            }
        }
        return false;
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

    private static Config clientConfig() {
        var agentConf = new ServiceConf();
        agentConf.addConnector(new Connector("127.0.0.1", masterPort, true));
        var config = new Config();
        config.getServiceConfMap().put(Zeze.MQ.Master.MasterAgent.eServiceName, agentConf);
        return config;
    }
}
