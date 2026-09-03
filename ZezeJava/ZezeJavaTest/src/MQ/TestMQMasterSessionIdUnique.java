package MQ;

import harness.Fast;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.MQ.MQ;
import Zeze.MQ.MQConsumer;
import Zeze.MQ.MQManager;
import Zeze.Net.Acceptor;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Master 重启（同 home）后 sessionIdGen 不得重发重启前已发过的 id：sessionId 不持久化，而
 * Manager 端订阅跨 Master 重启存活，重叠 id 会被 MQPartition.subscribe 的 putIfAbsent 静默
 * 吞掉，新消费者订阅"成功"却永久收不到消息（FND-G2-4）。
 * <p>
 * 客户端（MQConsumer 内的静态 agent）从默认 zeze.xml 读取 Zeze.MQ.Master.Agent 的 Connector，
 * master 端口必须保持 26000。
 */
@Fast
@Isolated // master 端口 26000 与 TestMQ 相同（MQConsumer 静态 agent 读默认 zeze.xml），类级并发下会端口冲突，独占运行
public class TestMQMasterSessionIdUnique {
    private static final int masterPort = 26000;

    @Test
    public void testSessionIdUniqueAcrossMasterRestart(@TempDir Path tempDir) throws Exception {
        Task.tryInitThreadPool();

        var masterHome = tempDir.resolve("mqmaster").toString();
        var master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
        var manager = new MQManager(tempDir.resolve("mqmanager").toString(), managerConfig(26001));
        var topic = "topicSessionIdUnique";
        try {
            master.start();
            manager.start();
            try {
                MQ.createMQ(topic, 1, new BOptions.Data(BOptions.Single));
            } catch (Exception ex) {
                // skip
            }

            // 重启前发两个 openMQ id（createMQ 也会发一个但客户端不可观测）。
            // 修复前的发号序列为 createMQ=1, openMQ=2,3；重启后 openMQ=1,2，其中 2 落在重启前
            // 集合内——重叠即静默饿死，断言失败可复现缺陷。
            var consumer1 = new MQConsumer(topic, m -> { });
            var consumer2 = new MQConsumer(topic, m -> { });
            var before1 = consumer1.getSessionId();
            var before2 = consumer2.getSessionId();
            consumer1.close();
            consumer2.close();
            var before = new HashSet<Long>();
            before.add(before1);
            before.add(before2);
            assertNotEquals(before1, before2);

            // 模拟 Master 崩溃重启（同 home），Manager 进程不重启，其内存订阅与自动重连均存活。
            master.stop();
            master = new Zeze.MQ.Master.Main(masterHome, masterConfig());
            master.start();

            var consumer3 = new MQConsumer(topic, m -> { });
            var consumer4 = new MQConsumer(topic, m -> { });
            var after1 = consumer3.getSessionId();
            var after2 = consumer4.getSessionId();
            consumer3.close();
            consumer4.close();

            assertNotEquals(after1, after2);
            assertFalse(before.contains(after1), "sessionId reused after master restart: " + after1);
            assertFalse(before.contains(after2), "sessionId reused after master restart: " + after2);
        } finally {
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
