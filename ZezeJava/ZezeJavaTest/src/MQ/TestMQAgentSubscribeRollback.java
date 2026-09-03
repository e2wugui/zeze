package MQ;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.MQ.MQ;
import Zeze.MQ.MQAgent;
import Zeze.MQ.MQConsumer;
import Zeze.MQ.MQManager;
import Zeze.MQ.MQPartition;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyServer;
import Zeze.Util.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQAgent.subscribe 半成功回滚与 unsubscribe 逐台 best-effort（FND-G2-6）：
 * 任一 Manager 不可达时订阅必须整体失败——已发出的 Subscribe 被撤销、本地 consumers 条目移除，
 * 不能留下"构造失败"的幽灵消费者继续收消息并 ack；unsubscribe 单台失败不得中断其余台，
 * 且 consumers.remove 必达。
 * <p>
 * case1 用独立 MQAgent 实例直测 subscribe 回滚（MQConsumer 构造内部走同一逻辑）；
 * case2 走 MQConsumer.close() 真实路径。客户端走默认 zeze.xml 的 26000 连 master。
 * <p>
 * 不可达地址的 GetReadySocket 各含一次 5 秒超时等待，不标 @Fast（integrationTest）。
 */
public class TestMQAgentSubscribeRollback {
    private static final int masterPort = 26000;
    // 无监听端口：连接拒绝，GetReadySocket 走满 5 秒超时。
    private static final int deadPort = 26299;

    @Test
    public void testSubscribeRollbackAndUnsubscribeBestEffort(@TempDir Path tempDir) throws Exception {
        Task.tryInitThreadPool();

        var master = new Zeze.MQ.Master.Main(tempDir.resolve("mqmaster").toString(), masterConfig());
        var manager = new MQManager(tempDir.resolve("mqmanager").toString(), managerConfig(26001));
        var topic = "topicSubscribeRollback";
        try {
            master.start();
            manager.start();
            try {
                MQ.createMQ(topic, 2, new BOptions.Data(BOptions.Single));
            } catch (Exception ex) {
                // skip
            }

            // 正常消费者：建立可回滚/退订断言的基线（sessionId1 在本地与 Manager 端均存活）。
            var consumer = new MQConsumer(topic, m -> { });
            var sessionId1 = consumer.getSessionId();
            assertTrue(MQConsumer.getConsumers().contains(consumer));
            assertTrue(partitionSubscribes(manager, topic).containsKey(sessionId1));

            // case1: 第二个 manager 不可达 -> subscribe 整体失败且无残留（修复前：good 侧
            // Subscribe 已生效、consumers 条目残留——幽灵消费者）。
            var agent = new MQAgent();
            agent.start();
            var goodManagerPort = consumerManagers(consumer).iterator().next().getPort();
            var goodConnector = agent.getOrAddConnector("127.0.0.1", goodManagerPort);
            var badConnector = agent.getOrAddConnector("127.0.0.1", deadPort);
            var mixed = new LinkedHashSet<Connector>(); // 保证 good 先发出、bad 超时中断
            mixed.add(goodConnector);
            mixed.add(badConnector);
            var sessionId2 = sessionId1 + 1;

            assertThrows(Exception.class, () -> agent.subscribe(topic, sessionId2, consumer, mixed));
            assertFalse(agent.getConsumers().containsKey(sessionId2), "consumers entry leaked on failed subscribe");
            assertFalse(partitionSubscribes(manager, topic).containsKey(sessionId2),
                    "ghost subscription left on manager after failed subscribe");
            assertTrue(partitionSubscribes(manager, topic).containsKey(sessionId1));

            // case2: unsubscribe 含不可达地址 -> best-effort 不抛、不残留（修复前：bad 超时抛出
            // 且不执行 consumers.remove）。
            var managersOfConsumer = consumerManagers(consumer);
            assertEquals(1, managersOfConsumer.size());
            managersOfConsumer.add(badConnector);
            assertDoesNotThrow(consumer::close);
            assertFalse(MQConsumer.getConsumers().contains(consumer), "consumers entry leaked on unsubscribe");
            assertTrue(partitionSubscribes(manager, topic).isEmpty(), "subscription left on manager after unsubscribe");
        } finally {
            manager.stop();
            master.stop();
        }
    }

    @SuppressWarnings("unchecked")
    private static HashSet<Connector> consumerManagers(MQConsumer consumer) throws Exception {
        var f = MQConsumer.class.getDeclaredField("managers");
        f.setAccessible(true);
        return (HashSet<Connector>)f.get(consumer);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Long, AsyncSocket> partitionSubscribes(MQManager manager, String topic) throws Exception {
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
