package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Commit.DummyImportBean;
import Zeze.Builtin.Dbh2.Commit.Prepare;
import Zeze.Config;
import org.rocksdb.RocksDBException;

public class Commit extends AbstractCommit {
    private CommitRocks rocks;
    private final CommitService service;

    public Commit(Dbh2AgentManager manager, Config config) throws RocksDBException {
        rocks = new CommitRocks(manager);
        service = new CommitService(config);
        RegisterProtocols(service);
    }

    public void start() throws Exception {
        service.start();
    }

    public synchronized void stop() throws Exception {
        if (null != rocks) {
            rocks.close();
            rocks = null;
        }
        service.stop();
    }

    public CommitRocks getRocks() {
        return rocks;
    }

    public CommitService getService() {
        return service;
    }

    @Override
    protected long ProcessCommitRequest(Zeze.Builtin.Dbh2.Commit.Commit r) throws Exception {
        rocks.commitTransaction(r.Argument.getTid(), r.Argument.getBuckets());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessDummyImportBean(DummyImportBean p) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessPrepareRequest(Prepare r) throws Exception {
        r.Result.setTid(rocks.prepare(r.Argument.getBuckets()));
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessQueryRequest(Zeze.Builtin.Dbh2.Commit.Query r) throws Exception {
        r.Result.setState(rocks.query(r.Argument.getTid()));
        r.SendResult();
        return 0;
    }
}
