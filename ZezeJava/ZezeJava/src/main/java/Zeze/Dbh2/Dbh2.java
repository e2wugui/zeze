package Zeze.Dbh2;

import java.io.Closeable;
import java.io.IOException;
import Zeze.Builtin.Dbh2.CommitTransaction;
import Zeze.Builtin.Dbh2.KeepAlive;
import Zeze.Builtin.Dbh2.RollbackTransaction;
import Zeze.Builtin.Dbh2.SetBucketMeta;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import Zeze.Util.RocksDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

public class Dbh2 extends AbstractDbh2 implements Closeable {
    private static final Logger logger = LogManager.getLogger(Dbh2.class);
    private final Dbh2Config config = new Dbh2Config();
    private final Raft raft;
    private final Dbh2StateMachine stateMachine;
    private final Dbh2Manager manager;

    public Raft getRaft() {
        return raft;
    }
    public Dbh2Manager getManager() {
        return manager;
    }

    public Dbh2(Dbh2Manager manager, String raftName, RaftConfig raftConf, Config config, boolean writeOptionSync) {
        this.manager = manager;

        if (config == null)
            config = new Config().addCustomize(this.config).loadAndParse();

        try {
            stateMachine = new Dbh2StateMachine();
            raft = new Raft(stateMachine, raftName, raftConf, config, "Zeze.Dbh2.Server", Zeze.Raft.Server::new);
            logger.info("newRaft: {}", raft.getName());
            stateMachine.openBucket();
            var writeOptions = writeOptionSync ? RocksDatabase.getSyncWriteOptions() : RocksDatabase.getDefaultWriteOptions();
            raft.getLogSequence().setWriteOptions(writeOptions);
            stateMachine.getBucket().setWriteOptions(writeOptions);

            RegisterProtocols(raft.getServer());
            raft.getServer().start();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("closeRaft: " + raft.getName());
        try {
            raft.shutdown();
            stateMachine.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected long ProcessBeginTransactionRequest(Zeze.Builtin.Dbh2.BeginTransaction r) {
        if (null != manager)
            manager.counterBeginTransaction.incrementAndGet();

        // 错误检查，防止访问桶错乱。
        if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
            return errorCode(eBucketMissmatch);

        // allocate tid
        var tid = stateMachine.getTidAllocator().next(stateMachine);

        r.Argument.setTransactionId(tid); // setup first
        var log = new LogBeginTransaction(r);
        r.Result.setTransactionId(tid); // prepare result before appendLog,
        raft.appendLog(log, r.Result);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessCommitTransactionRequest(CommitTransaction r) throws Exception {
        if (null != manager)
            manager.counterCommitTransaction.incrementAndGet();

        var log = new LogCommitTransaction(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessRollbackTransactionRequest(RollbackTransaction r) throws Exception {
        if (null != manager)
            manager.counterRollbackTransaction.incrementAndGet();

        var log = new LogRollbackTransaction(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessSetBucketMetaRequest(SetBucketMeta r) throws Exception {
        var log = new LogSetBucketMeta(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessDeleteRequest(Zeze.Builtin.Dbh2.Delete r) {
        if (null != manager)
            manager.counterDelete.incrementAndGet();

        if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
            return errorCode(eBucketMissmatch);

        var log = new LogDelete(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException {
        if (null != manager)
            manager.counterGet.incrementAndGet();

        // 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
        var bucket = stateMachine.getBucket();
        if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
            return errorCode(eBucketMissmatch);
        var value = bucket.get(r.Argument.getKey());
        if (null == value)
            r.Result.setNull(true);
        else {
            r.Result.setValue(new Binary(value));
            if (null != manager)
                manager.sizeGet.addAndGet(value.length);
        }
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessKeepAliveRequest(KeepAlive r) throws Exception {
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessPutRequest(Zeze.Builtin.Dbh2.Put r) {
        if (null != manager) {
            manager.counterPut.incrementAndGet();
            manager.sizePut.addAndGet(r.Argument.getValue().size());
        }
        if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
            return errorCode(eBucketMissmatch);

        var log = new LogPut(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }
}
