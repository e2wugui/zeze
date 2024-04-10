// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractGlobalCacheManagerWithRaftAgent implements Zeze.IModule {
    public static final int ModuleId = 11001;
    public static final String ModuleName = "GlobalCacheManagerWithRaftAgent";
    public static final String ModuleFullName = "Zeze.Services.GlobalCacheManagerWithRaftAgent";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcquireResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAcquireResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47251404755902L, factoryHandle); // 11001, -1825434690
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCleanupResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCleanupResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47253156226169L, factoryHandle); // 11001, -73964423
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive.class, Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47249886857671L, factoryHandle); // 11001, 951634375
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Login.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Login::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47251261574418L, factoryHandle); // 11001, -1968616174
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose.class, Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNormalCloseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47249192987366L, factoryHandle); // 11001, 257764070
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce::new;
            factoryHandle.Handle = this::ProcessReduceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReduceRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47250386526035L, factoryHandle); // 11001, 1451302739
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin.class, Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47251807618150L, factoryHandle); // 11001, -1422572442
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
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

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessReduceRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce r) throws Exception;
}
