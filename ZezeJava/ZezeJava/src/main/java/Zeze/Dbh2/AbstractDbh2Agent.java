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

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.BeginTransaction.class, Zeze.Builtin.Dbh2.BeginTransaction.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.BeginTransaction::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBeginTransactionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBeginTransactionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360124156483L, factoryHandle); // 11026, -480216509
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.CommitTransaction.class, Zeze.Builtin.Dbh2.CommitTransaction.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.CommitTransaction::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitTransactionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitTransactionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47359122130965L, factoryHandle); // 11026, -1482242027
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Delete.class, Zeze.Builtin.Dbh2.Delete.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Delete::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDeleteResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessDeleteResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360236597486L, factoryHandle); // 11026, -367775506
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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Put.class, Zeze.Builtin.Dbh2.Put.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Put::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPutResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPutResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47359688675419L, factoryHandle); // 11026, -915697573
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.RollbackTransaction.class, Zeze.Builtin.Dbh2.RollbackTransaction.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.RollbackTransaction::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRollbackTransactionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRollbackTransactionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360280866090L, factoryHandle); // 11026, -323506902
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.GetBuckets.class, Zeze.Builtin.Dbh2.Master.GetBuckets.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.GetBuckets::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetBucketsResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetBucketsResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363118214025L, factoryHandle); // 11027, -1781126263
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Master.LocateBucket.class, Zeze.Builtin.Dbh2.Master.LocateBucket.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Master.LocateBucket::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLocateBucketResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLocateBucketResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47363709711447L, factoryHandle); // 11027, -1189628841
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47360124156483L);
        service.getFactorys().remove(47359122130965L);
        service.getFactorys().remove(47360236597486L);
        service.getFactorys().remove(47356839198180L);
        service.getFactorys().remove(47358800944088L);
        service.getFactorys().remove(47359688675419L);
        service.getFactorys().remove(47360280866090L);
        service.getFactorys().remove(47363118214025L);
        service.getFactorys().remove(47363709711447L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
