// auto-generated @formatter:off
package Zeze.Dbh2;

public abstract class AbstractCommit implements Zeze.IModule {
    public static final int ModuleId = 11028;
    public static final String ModuleName = "Commit";
    public static final String ModuleFullName = "Zeze.Dbh2.Commit";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eCommitNotExist = 0;
    public static final int ePreparing = 1;
    public static final int eCommitting = 2;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Commit.Commit.class, Zeze.Builtin.Dbh2.Commit.Commit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Commit.Commit::new;
            factoryHandle.Handle = this::ProcessCommitRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47365570898711L, factoryHandle); // 11028, 671558423
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Commit.DummyImportBean.class, Zeze.Builtin.Dbh2.Commit.DummyImportBean.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Commit.DummyImportBean::new;
            factoryHandle.Handle = this::ProcessDummyImportBean;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDummyImportBean", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessDummyImportBean", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47368749528429L, factoryHandle); // 11028, -444779155
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Commit.Query.class, Zeze.Builtin.Dbh2.Commit.Query.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Commit.Query::new;
            factoryHandle.Handle = this::ProcessQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47365186843239L, factoryHandle); // 11028, 287502951
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47365570898711L);
        service.getFactorys().remove(47368749528429L);
        service.getFactorys().remove(47365186843239L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCommitRequest(Zeze.Builtin.Dbh2.Commit.Commit r) throws Exception;
    protected abstract long ProcessDummyImportBean(Zeze.Builtin.Dbh2.Commit.DummyImportBean p) throws Exception;
    protected abstract long ProcessQueryRequest(Zeze.Builtin.Dbh2.Commit.Query r) throws Exception;
}
