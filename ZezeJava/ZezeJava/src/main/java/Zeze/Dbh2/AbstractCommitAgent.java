// auto-generated @formatter:off
package Zeze.Dbh2;

public abstract class AbstractCommitAgent implements Zeze.IModule {
    public static final int ModuleId = 11028;
    public static final String ModuleName = "CommitAgent";
    public static final String ModuleFullName = "Zeze.Dbh2.CommitAgent";

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
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47365570898711L, factoryHandle); // 11028, 671558423
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Commit.Query.class, Zeze.Builtin.Dbh2.Commit.Query.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Commit.Query::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47365186843239L, factoryHandle); // 11028, 287502951
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47365570898711L);
        service.getFactorys().remove(47365186843239L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
