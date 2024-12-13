package Zeze.MQ;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.MQ.Master.MasterAgent;
import Zeze.Raft.ProxyServer;
import Zeze.Util.KV;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MQManager extends AbstractMQManager {
    private static final Logger logger = LogManager.getLogger();

    private final Service masterService;
    private final MasterAgent masterAgent;
    private final ProxyServer proxyServer;
    private final String home;
    private final MQConfig mqConfig = new MQConfig();
    private Future<?> loadMonitorTimer;

    // 本manager的所有队列实现。
    // { topic -> { partitionIndex -> MQFile } }
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, MQFile>> mqFiles = new ConcurrentHashMap<>();

    public MQManager(String home, String configXml) {
        this.home = home;
        var config = Config.load(configXml);
        config.parseCustomize(this.mqConfig);
        proxyServer = new ProxyServer(config, mqConfig.getRpcTimeout());
        masterService = new Service(config, proxyServer);
        masterAgent = new MasterAgent(config, masterService);
    }

    public MQConfig getMqConfig() {
        return mqConfig;
    }

    public MasterAgent getMasterAgent() {
        return masterAgent;
    }

    public int queueCount() {
        int count = 0;
        for (var mqs : mqFiles.values())
            count += mqs.size();
        return count;
    }

    public void start() throws Exception {
        ShutdownHook.add(this, this::stop);
        logger.info("start MQManager from '{}'", home);
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
    }

    private void loadMonitor() {
        var loadManager = 0.0;
        for (var mqs : mqFiles.values()) {
            for (var mqFile : mqs.values()) {
                var load = mqFile.load();
                loadManager += load;
            }
        }
        masterAgent.reportLoad(loadManager);
    }

    @Override
    protected long ProcessSendMessageRequest(Zeze.Builtin.MQ.SendMessage r) {
        // todo 这里mqFile应该已经创建好了，需要master通知manager创建。这里临时使用putIfAbsent。
        var mqsNew = new ConcurrentHashMap<Integer, MQFile>();
        var mqs = mqFiles.putIfAbsent(r.Argument.getTopic(), mqsNew);
        if (mqs == null)
            mqs = mqsNew;
        var mqFileNew = new MQFile();
        var mqFile = mqs.putIfAbsent(r.Argument.getPartitionIndex(), mqFileNew);
        if (mqFile == null)
            mqFile = mqFileNew;
        mqFile.sendMessage(r.Argument);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessSubscribeRequest(Zeze.Builtin.MQ.Subscribe r) {
        return Zeze.Transaction.Procedure.NotImplement;
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
