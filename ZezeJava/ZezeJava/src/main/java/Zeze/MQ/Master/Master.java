package Zeze.MQ.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Builtin.MQ.Master.CreateMQ;
import Zeze.Builtin.MQ.Master.CreatePartition;
import Zeze.Builtin.MQ.Master.ReportLoad;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.Register;
import Zeze.Builtin.MQ.Master.Subscribe;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public class Master extends AbstractMaster {
    private static final Logger logger = LogManager.getLogger();
    public static final String MasterDbName = "__mq_master__";
    private final String home;
    private final RocksDatabase masterDb;
    private final RocksDatabase.Table mqTable;
    private final Config zezeConfig;
    private final AtomicLong sessionIdGen = new AtomicLong();

    public static class Manager {
        private final AsyncSocket socket;
        private final BMQServer.Data info;
        private double load;
        private final HashSet<Integer> partitionIndexes = new HashSet<>();

        public Manager(AsyncSocket socket, BMQServer.Data data) {
            this.socket = socket;
            this.info = data;
        }
    }

    private final ArrayList<Manager> managers = new ArrayList<>();

    public Master(String home, Config zezeConfig) throws RocksDBException {
        this.home = home;
        this.zezeConfig = zezeConfig;
        masterDb = new RocksDatabase(Path.of(home, MasterDbName).toString(),
                RocksDatabase.DbType.eOptimisticTransactionDb);
        mqTable = masterDb.getOrAddTable("mq");
    }

    public void close() {
        masterDb.close();
    }

    public String getHome() {
        return home;
    }

    public Config getZezeConfig() {
        return zezeConfig;
    }

    public void tryRemoveManager(AsyncSocket manager) {
        lock();
        try {
            for (int i = 0; i < managers.size(); ++i) {
                var e = managers.get(i);
                if (e.socket == manager) {
                    managers.remove(i);
                    break;
                }
            }
        } finally {
            unlock();
        }
    }

    private Manager[] choiceManager(int hint) {
        // todo load
        return managers.toArray(new Manager[managers.size()]);
    }

    @Override
    protected long ProcessCreateMQRequest(CreateMQ r) throws Exception {
        if (r.Argument.getTopic().contains("."))
            return errorCode(eTopicHasReserveChar);
        if (r.Argument.getTopic().contains("/"))
            return errorCode(eTopicHasReserveChar);
        if (r.Argument.getTopic().contains("\\"))
            return errorCode(eTopicHasReserveChar);
        var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
        var mq = mqTable.get(topicBytes);
        if (null != mq)
            return errorCode(eTopicExist);

        var servers = r.Result;

        // create
        servers.getInfo().setTopic(r.Argument.getTopic());
        servers.getInfo().setPartition(r.Argument.getPartition());
        servers.getInfo().setOptions(r.Argument.getOptions());
        if (r.Argument.getPartition() < 1)
            return errorCode(ePartition);

        servers.setSessionId(sessionIdGen.incrementAndGet());

        // 分配manager
        var managers = choiceManager(r.Argument.getPartition());
        for (var i = 0; i < r.Argument.getPartition(); ++i) {
            var manager = managers[i % managers.length];
            var info = manager.info.copy();
            info.setPartitionIndex(i);
            info.setTopic(r.Argument.getTopic());
            servers.getServers().add(info);
            manager.partitionIndexes.add(i);
        }
        for (var manager : managers) {
            var cp = new CreatePartition();
            cp.Argument.setTopic(r.Argument.getTopic());
            cp.Argument.setPartitionIndexes(manager.partitionIndexes);
            cp.SendForWait(manager.socket).await();
            if (cp.getResultCode() != 0) {
                logger.error("create partition error={} r={}", IModule.getErrorCode(cp.getResultCode()), cp);
                return errorCode(eCreatePartition);
            }
        }
        // save mq info
        var value = ByteBuffer.Allocate();
        servers.encode(value);
        mqTable.put(topicBytes, 0, topicBytes.length, value.Bytes, value.ReadIndex, value.size());

        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessOpenMQRequest(Zeze.Builtin.MQ.Master.OpenMQ r) throws Exception {
        var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
        var mq = mqTable.get(topicBytes);
        if (null == mq)
            return errorCode(eTopicNotExist);
        var servers = r.Result;
        servers.decode(ByteBuffer.Wrap(mq));
        servers.setSessionId(sessionIdGen.incrementAndGet()); // 生成id，必须在decode之后设置。
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessRegisterRequest(Register r) {
        lock();
        try {
            managers.add(new Manager(r.getSender(), r.Argument));
            r.SendResult();
            return 0;
        } finally {
            unlock();
        }
    }

    private Manager findManager(AsyncSocket sender) {
        for (var manager : managers)
            if (manager.socket == sender)
                return manager;
        return null;
    }

    @Override
    protected long ProcessReportLoadRequest(ReportLoad r) {
        lock();
        try {
            var manager = findManager(r.getSender());
            if (null == manager)
                return errorCode(eManagerNotFound);
            manager.load = r.Argument.getLoad();
            r.SendResult();
            return 0;
        } finally {
            unlock();
        }
    }

    @Override
    protected long ProcessSubscribeRequest(Subscribe r) throws Exception {
        var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
        var mq = mqTable.get(topicBytes);
        var servers = r.Result;
        if (null == mq)
            return errorCode(eTopicNotExist);
        servers.decode(ByteBuffer.Wrap(mq));
        r.SendResult();
        return 0;
    }
}
