// auto-generated @formatter:off
package Zeze.Dbh2;

public abstract class AbstractDbh2 implements Zeze.IModule {
    public static final int ModuleId = 11026;
    public static final String ModuleName = "Dbh2";
    public static final String ModuleFullName = "Zeze.Dbh2.Dbh2";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public static final int eBucketNotFound = 1;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.BeginTransaction.class, Zeze.Builtin.Dbh2.BeginTransaction.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.BeginTransaction::new;
            factoryHandle.Handle = this::ProcessBeginTransactionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBeginTransactionRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBeginTransactionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360124156483L, factoryHandle); // 11026, -480216509
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.CommitTransaction.class, Zeze.Builtin.Dbh2.CommitTransaction.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.CommitTransaction::new;
            factoryHandle.Handle = this::ProcessCommitTransactionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitTransactionRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitTransactionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47359122130965L, factoryHandle); // 11026, -1482242027
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Delete.class, Zeze.Builtin.Dbh2.Delete.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Delete::new;
            factoryHandle.Handle = this::ProcessDeleteRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDeleteRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessDeleteRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360236597486L, factoryHandle); // 11026, -367775506
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Get.class, Zeze.Builtin.Dbh2.Get.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Get::new;
            factoryHandle.Handle = this::ProcessGetRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47356839198180L, factoryHandle); // 11026, 529792484
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.KeepAlive.class, Zeze.Builtin.Dbh2.KeepAlive.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.KeepAlive::new;
            factoryHandle.Handle = this::ProcessKeepAliveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47358800944088L, factoryHandle); // 11026, -1803428904
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.Put.class, Zeze.Builtin.Dbh2.Put.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.Put::new;
            factoryHandle.Handle = this::ProcessPutRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPutRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPutRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47359688675419L, factoryHandle); // 11026, -915697573
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.RollbackTransaction.class, Zeze.Builtin.Dbh2.RollbackTransaction.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.RollbackTransaction::new;
            factoryHandle.Handle = this::ProcessRollbackTransactionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRollbackTransactionRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRollbackTransactionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47360280866090L, factoryHandle); // 11026, -323506902
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Dbh2.UseDataRefDummy.class, Zeze.Builtin.Dbh2.UseDataRefDummy.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Dbh2.UseDataRefDummy::new;
            factoryHandle.Handle = this::ProcessUseDataRefDummy;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUseDataRefDummy", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUseDataRefDummy", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47357107631101L, factoryHandle); // 11026, 798225405
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
        service.getFactorys().remove(47357107631101L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessBeginTransactionRequest(Zeze.Builtin.Dbh2.BeginTransaction r) throws Exception;
    protected abstract long ProcessCommitTransactionRequest(Zeze.Builtin.Dbh2.CommitTransaction r) throws Exception;
    protected abstract long ProcessDeleteRequest(Zeze.Builtin.Dbh2.Delete r) throws Exception;
    protected abstract long ProcessGetRequest(Zeze.Builtin.Dbh2.Get r) throws Exception;
    protected abstract long ProcessKeepAliveRequest(Zeze.Builtin.Dbh2.KeepAlive r) throws Exception;
    protected abstract long ProcessPutRequest(Zeze.Builtin.Dbh2.Put r) throws Exception;
    protected abstract long ProcessRollbackTransactionRequest(Zeze.Builtin.Dbh2.RollbackTransaction r) throws Exception;
    protected abstract long ProcessUseDataRefDummy(Zeze.Builtin.Dbh2.UseDataRefDummy p) throws Exception;
}
