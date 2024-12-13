package Zeze.MQ.Master;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Builtin.MQ.Master.ReportLoad;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.Register;
import Zeze.Builtin.MQ.Master.Subscribe;
import Zeze.Config;
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

    public static class Manager {
        private final AsyncSocket socket;
        private final BMQServer.Data info;
        private double load;

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
    protected long ProcessOpenMQRequest(Zeze.Builtin.MQ.Master.OpenMQ r) throws Exception {
        var topicBytes = r.Argument.getTopic().getBytes(StandardCharsets.UTF_8);
        var mq = mqTable.get(topicBytes);
        var servers = r.Result;

        if (null == mq) {
            // create
            servers.getInfo().setTopic(r.Argument.getTopic());
            servers.getInfo().setPartition(r.Argument.getPartition());
            servers.getInfo().setOptions(r.Argument.getOptions());
            if (r.Argument.getPartition() < 1)
                return errorCode(ePartition);
            // 分配manager
            var managers = choiceManager(r.Argument.getPartition());
            for (var i = 0; i < r.Argument.getPartition(); ++i) {
                var manager = managers[i % managers.length];
                var info = manager.info.copy();
                info.setPartitionIndex(i);
                servers.getServers().add(info);
            }
        } else {
            // alter
            servers.decode(ByteBuffer.Wrap(mq));
            if (r.Argument.getPartition() <= servers.getInfo().getPartition())
                return errorCode(ePartition);
            var hintAdd = r.Argument.getPartition() - servers.getInfo().getPartition();
            // alter忽略options.
            // 分配增加的manager
            var managers = choiceManager(hintAdd);
            for (var i = 0; i < hintAdd; ++i) {
                var manager = managers[i % managers.length];
                var info = manager.info.copy();
                info.setPartitionIndex(i + servers.getInfo().getPartition());
                servers.getServers().add(info);
            }
            servers.getInfo().setPartition(r.Argument.getPartition());
        }
        // save mq info
        var value = ByteBuffer.Allocate();
        servers.encode(value);
        mqTable.put(topicBytes, 0, topicBytes.length, value.Bytes, value.ReadIndex, value.size());
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
