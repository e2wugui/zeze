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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.rocksdb.RocksDBException;

public class Dbh2 extends AbstractDbh2 implements Closeable {
    private final Dbh2Config config = new Dbh2Config();
    private final Raft raft;
    private final Dbh2StateMachine stateMachine;

    public Dbh2(String raftName, RaftConfig raftConf, Config config, boolean writeOptionSync) throws Exception {
        if (config == null)
            config = new Config().addCustomize(this.config).loadAndParse();

        stateMachine = new Dbh2StateMachine();
        raft = new Raft(stateMachine, raftName, raftConf, config, "Zeze.Dbh2.Server", Zeze.Raft.Server::new);
        stateMachine.openBucket();
        var writeOptions = writeOptionSync ? Bucket.getSyncWriteOptions() : Bucket.getDefaultWriteOptions();
        raft.getLogSequence().setWriteOptions(writeOptions);

        RegisterProtocols(raft.getServer());
        raft.getServer().start();
    }

    @Override
    public void close() throws IOException {
        try {
            raft.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected long ProcessBeginTransactionRequest(Zeze.Builtin.Dbh2.BeginTransaction r) {
        // 错误检查，防止访问桶错乱。
        if (!stateMachine.getBucket().inBucket(r.Argument.getDatabase(), r.Argument.getTable()))
            return errorCode(eBucketMissmatch);

        // allocate tid
        var tid = 0; // todo allocate
        r.Argument.setTransactionId(tid); // setup first
        var log = new LogBeginTransaction(r);
        r.Result.setTransactionId(tid); // prepare result before appendLog,
        raft.appendLog(log, r.Result);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessCommitTransactionRequest(CommitTransaction r) throws Exception {
        var log = new LogCommitTransaction(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessRollbackTransactionRequest(RollbackTransaction r) throws Exception {
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
        var log = new LogDelete(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws RocksDBException {
        // 直接读取数据库。是否可以读取由raft控制。raft启动时有准备阶段。
        var bucket = stateMachine.getBucket();
        if (!bucket.inBucket(r.Argument.getDatabase(), r.Argument.getTable(), r.Argument.getKey()))
            return errorCode(eBucketMissmatch);
        var value = bucket.get(r.Argument.getKey());
        if (null == value)
            r.Result.setNull(true);
        else
            r.Result.setValue(new Binary(value));
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
        var log = new LogPut(r);
        raft.appendLog(log, r.Result); // result is empty
        r.SendResult();
        return 0;
    }
}
