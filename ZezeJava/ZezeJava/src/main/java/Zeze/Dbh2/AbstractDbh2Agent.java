// auto-generated @formatter:off
package Zeze.Dbh2;

public abstract class AbstractDbh2Agent implements Zeze.IModule {
    public static final int ModuleId = 11026;
    public static final String ModuleName = "Dbh2Agent";
    public static final String ModuleFullName = "Zeze.Dbh2.Dbh2Agent";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eBucketNotFound = 1;
    public static final int eBucketMismatch = 2;
    public static final int eDuplicateTid = 3;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.CommitBatch.class, Zeze.Builtin.Dbh2.CommitBatch.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.CommitBatch::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitBatchResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitBatchResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47357049712520L, factoryHandle); // 11026, 740306824
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Get.class, Zeze.Builtin.Dbh2.Get.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Get::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47356839198180L, factoryHandle); // 11026, 529792484
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.KeepAlive.class, Zeze.Builtin.Dbh2.KeepAlive.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.KeepAlive::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47358800944088L, factoryHandle); // 11026, -1803428904
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.PrepareBatch.class, Zeze.Builtin.Dbh2.PrepareBatch.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.PrepareBatch::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPrepareBatchResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPrepareBatchResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360344602230L, factoryHandle); // 11026, -259770762
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.SetBucketMeta.class, Zeze.Builtin.Dbh2.SetBucketMeta.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.SetBucketMeta::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetBucketMetaResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetBucketMetaResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47356909547647L, factoryHandle); // 11026, 600141951
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.SplitPut.class, Zeze.Builtin.Dbh2.SplitPut.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.SplitPut::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSplitPutResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSplitPutResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47359148214035L, factoryHandle); // 11026, -1456158957
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.UndoBatch.class, Zeze.Builtin.Dbh2.UndoBatch.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.UndoBatch::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUndoBatchResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUndoBatchResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47357555155327L, factoryHandle); // 11026, 1245749631
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Walk.class, Zeze.Builtin.Dbh2.Walk.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Walk::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessWalkResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessWalkResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47356866459183L, factoryHandle); // 11026, 557053487
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.WalkKey.class, Zeze.Builtin.Dbh2.WalkKey.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.WalkKey::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessWalkKeyResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessWalkKeyResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47357793622820L, factoryHandle); // 11026, 1484217124
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47357049712520L);
        service.getFactorys().remove(47356839198180L);
        service.getFactorys().remove(47358800944088L);
        service.getFactorys().remove(47360344602230L);
        service.getFactorys().remove(47356909547647L);
        service.getFactorys().remove(47359148214035L);
        service.getFactorys().remove(47357555155327L);
        service.getFactorys().remove(47356866459183L);
        service.getFactorys().remove(47357793622820L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
