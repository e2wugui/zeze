// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractGlobalCacheManagerWithRaft extends Zeze.IModule {
    public static final int ModuleId = 11001;
    @Override public String getFullName() { return "Zeze.Services.GlobalCacheManagerWithRaft"; }
    @Override public String getName() { return "GlobalCacheManagerWithRaft"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire::new;
            factoryHandle.Handle = this::ProcessAcquireRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcquireRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251404755902L, factoryHandle); // 11001, -1825434690
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup::new;
            factoryHandle.Handle = this::ProcessCleanupRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCleanupRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47253156226169L, factoryHandle); // 11001, -73964423
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive::new;
            factoryHandle.Handle = this::ProcessKeepAliveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47249886857671L, factoryHandle); // 11001, 951634375
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.Login>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251261574418L, factoryHandle); // 11001, -1968616174
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose::new;
            factoryHandle.Handle = this::ProcessNormalCloseRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47249192987366L, factoryHandle); // 11001, 257764070
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47250386526035L, factoryHandle); // 11001, 1451302739
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin>();
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251807618150L, factoryHandle); // 11001, -1422572442
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47251404755902L);
        service.getFactorys().remove(47253156226169L);
        service.getFactorys().remove(47249886857671L);
        service.getFactorys().remove(47251261574418L);
        service.getFactorys().remove(47249192987366L);
        service.getFactorys().remove(47250386526035L);
        service.getFactorys().remove(47251807618150L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.RegisterTableTemplate("Global", Zeze.Net.Binary.class, Zeze.Builtin.GlobalCacheManagerWithRaft.CacheState.class);
        rocks.RegisterTableTemplate("Session", Zeze.Net.Binary.class, Zeze.Builtin.GlobalCacheManagerWithRaft.AcquiredState.class);
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Integer.class));
    }

    protected abstract long ProcessAcquireRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire r) throws Throwable;
    protected abstract long ProcessCleanupRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup r) throws Throwable;
    protected abstract long ProcessKeepAliveRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive r) throws Throwable;
    protected abstract long ProcessLoginRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Login r) throws Throwable;
    protected abstract long ProcessNormalCloseRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose r) throws Throwable;
    protected abstract long ProcessReLoginRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin r) throws Throwable;
}
