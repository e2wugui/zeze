package Zeze.MQ;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Config;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Raft.ProxyServer;
import Zeze.Util.KV;
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import static Zeze.MQ.Master.AbstractMaster.ePartition;
import static Zeze.MQ.Master.AbstractMaster.eTopicNotExist;

public class MQManager extends AbstractMQManager {
    private static final Logger logger = LogManager.getLogger();

    private final Service masterService;
    private final MasterAgent masterAgent;
    private final ProxyServer proxyServer;
    private final String home;
    private final MQConfig mqConfig = new MQConfig();
    private Future<?> loadMonitorTimer;
    private RocksDatabase rocksDatabase;

    public RocksDatabase getRocksDatabase() {
        return rocksDatabase;
    }

    // 本manager的所有队列实现。
    // { topic -> { partitionIndex -> MQFile } }
    private final ConcurrentHashMap<String, MQPartition> queues = new ConcurrentHashMap<>();

    public MQManager(String home, String configXml) throws RocksDBException {
        this.home = home;
        this.rocksDatabase = new RocksDatabase(this.home);
        var config = Config.load(configXml);
        config.parseCustomize(this.mqConfig);
        proxyServer = new ProxyServer(config, mqConfig.getRpcTimeout());
        masterService = new Service(config, proxyServer);
        masterAgent = new MasterAgent(config, masterService, this::createPartition);
        RegisterProtocols(proxyServer);
    }

    public String getHome() {
        return home;
    }

    public MQConfig getMqConfig() {
        return mqConfig;
    }

    public MasterAgent getMasterAgent() {
        return masterAgent;
    }

    public int queueCount() {
        int count = 0;
        for (var queue : queues.values())
            count += queue.size();
        return count;
    }

    public void start() throws Exception {
        ShutdownHook.add(this, this::stop);
        logger.info("start MQManager from '{}'", home);
        loadMQ();
        masterAgent.startAndWaitConnectionReady();
        var acceptorAddress = masterService.getAcceptorAddress();
        masterAgent.register(acceptorAddress.getKey(), acceptorAddress.getValue(), queueCount());
        proxyServer.start();

        loadMonitorTimer = Task.scheduleUnsafe(120_000, 120_000, this::loadMonitor);
    }

    public void stop() throws Exception {
        if (null != loadMonitorTimer)
            loadMonitorTimer.cancel(true);
        ShutdownHook.remove(this);
        proxyServer.stop();
        masterAgent.stop();
        for (var queue : queues.values())
            queue.close();
    }

    private void loadMonitor() {
        var loadManager = 0.0;
        for (var queue : queues.values()) {
            loadManager += queue.load();
        }
        masterAgent.reportLoad(loadManager);
    }

    private void loadMQ() throws IOException {
        var topics = new File(home).listFiles();
        if (null == topics)
            return ;

        for (var topic : topics) {
            if (topic.isDirectory()) {
                var partitions = topic.listFiles();
                if (null == partitions)
                    continue;
                var partitionIndexes = new HashSet<Integer>();
                for (var partition : partitions) {
                    if (partition.isFile()) {
                        // 相同分区的文件可能有多个，这里使用HashSet会去重。
                        var pa = partition.getName().split("\\.");
                        if (pa.length == 2)
                            partitionIndexes.add(Integer.parseInt(pa[0]));
                    }
                }
                createPartition(topic.getName(), partitionIndexes);
            }
        }
    }

    private void createPartition(String topic, HashSet<Integer> partitionIndexes) {
        var cp = queues.computeIfAbsent(topic, (key) -> new MQPartition(this));
        var topicDir = new File(home, topic);
        //noinspection ResultOfMethodCallIgnored
        topicDir.mkdirs();
        cp.createPartitions(topic, partitionIndexes);
    }

    protected long createPartition(CreatePartition r) throws IOException {
        createPartition(r.Argument.getTopic(), r.Argument.getPartitionIndexes());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessSendMessageRequest(Zeze.Builtin.MQ.SendMessage r) {
        var queue = queues.get(r.Argument.getTopic());
        if (queue == null)
            return errorCode(eTopicNotExist);
        var partition = queue.get(r.Argument.getPartitionIndex());
        if (partition == null)
            return errorCode(ePartition);
        partition.sendMessage(r.Argument);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessSubscribeRequest(Zeze.Builtin.MQ.Subscribe r) {
        var queue = queues.get(r.Argument.getTopic());
        if (queue == null)
            return errorCode(eTopicNotExist);
        queue.subscribe(r.getSender(), r.Argument.getSessionId());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessUnsubscribeRequest(Zeze.Builtin.MQ.Unsubscribe r) {
        var queue = queues.get(r.Argument.getTopic());
        if (queue == null)
            return errorCode(eTopicNotExist);
        queue.unsubscribe(r.getSender(), r.Argument.getSessionId());
        r.SendResult();
        return 0;
    }

    public static class Service extends Zeze.MQ.Master.MasterAgent.Service {
        private final ProxyServer proxyServer;

        public Service(Config config) {
            super(config);
            proxyServer = null;
        }

        public Service(Config config, ProxyServer proxyServer) {
            super(config);
            this.proxyServer = proxyServer;
        }

        public KV<String, Integer> getAcceptorAddress() {
            // 优先查找代理配置，
            return null != proxyServer
                    ? proxyServer.getOneAcceptorAddress()
                    : getOneAcceptorAddress();
        }
    }
}
