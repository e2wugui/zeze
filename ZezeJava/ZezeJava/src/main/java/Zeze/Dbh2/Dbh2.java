package Zeze.Dbh2;

import java.io.Closeable;
import java.io.IOException;
import Zeze.Builtin.Dbh2.CommitTransaction;
import Zeze.Builtin.Dbh2.RollbackTransaction;
import Zeze.Config;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.rocksdb.WriteOptions;

public class Dbh2 extends AbstractDbh2 implements Closeable {
    private static final Logger logger = LogManager.getLogger(Dbh2.class);
    private final Dbh2Config config = new Dbh2Config();
    private final Raft raft;
    private final StateMachine stateMachine;

    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
        ((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
    }

    public Dbh2(String raftName, RaftConfig raftConf, Config config, boolean writeOptionSync) throws Exception {
        if (config == null)
            config = new Config().addCustomize(this.config).loadAndParse();

        stateMachine = new StateMachine();
        raft = new Raft(stateMachine, raftName, raftConf, config, "Zeze.Dbh2.Server", Zeze.Raft.Server::new);
        var writeOptions = writeOptionSync ? new WriteOptions().setSync(true) : new WriteOptions();
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
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessCommitTransactionRequest(CommitTransaction r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessRollbackTransactionRequest(RollbackTransaction r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessDeleteRequest(Zeze.Builtin.Dbh2.Delete r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessPutRequest(Zeze.Builtin.Dbh2.Put r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
