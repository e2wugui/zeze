package Zeze.Dbh2;

import Zeze.Config;
import org.rocksdb.RocksDBException;

public class Commit extends AbstractCommit {
    private final CommitRocks rocks;
    private final CommitService service;

    public Commit(Dbh2AgentManager manager, Config config) throws RocksDBException {
        rocks = new CommitRocks(manager);
        service = new CommitService(config);
        RegisterProtocols(service);
    }

    public void start() throws Exception {
        service.start();
    }

    public void stop() throws Exception {
        service.stop();
    }

    public CommitRocks getRocks() {
        return rocks;
    }

    @Override
    protected long ProcessCommitRequest(Zeze.Builtin.Dbh2.Commit.Commit r) throws Exception {
        var trans = CommitRocks.decodeTransaction(r.Argument.getTransactionData());
        rocks.commitTransaction(trans);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessQueryRequest(Zeze.Builtin.Dbh2.Commit.Query r) throws Exception {
        r.setResultCode(rocks.query(r.Argument.getTransactionKey()));
        r.SendResult();
        return 0;
    }
}
