// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractGlobalCacheManagerWithRaft implements Zeze.IModule {
    public static final int ModuleId = 11001;
    public static final String ModuleName = "GlobalCacheManagerWithRaft";
    public static final String ModuleFullName = "Zeze.Services.GlobalCacheManagerWithRaft";

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
            factoryHandle.Handle = this::ProcessAcquireRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcquireRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAcquireRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47251404755902L, factoryHandle); // 11001, -1825434690
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup::new;
            factoryHandle.Handle = this::ProcessCleanupRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCleanupRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCleanupRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47253156226169L, factoryHandle); // 11001, -73964423
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive.class, Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive::new;
            factoryHandle.Handle = this::ProcessKeepAliveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47249886857671L, factoryHandle); // 11001, 951634375
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Login.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47251261574418L, factoryHandle); // 11001, -1968616174
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose.class, Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose::new;
            factoryHandle.Handle = this::ProcessNormalCloseRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNormalCloseRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47249192987366L, factoryHandle); // 11001, 257764070
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce.class, Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReduceResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReduceResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47250386526035L, factoryHandle); // 11001, 1451302739
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin.class, Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReLoginRequest", Zeze.Transaction.DispatchMode.Normal);
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
        rocks.registerTableTemplate("Global", Zeze.Net.Binary.class, Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState.class);
        rocks.registerTableTemplate("Session", Zeze.Net.Binary.class, Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquiredState.class);
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Integer.class));
    }

    protected abstract long ProcessAcquireRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Acquire r) throws Exception;
    protected abstract long ProcessCleanupRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Cleanup r) throws Exception;
    protected abstract long ProcessKeepAliveRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.KeepAlive r) throws Exception;
    protected abstract long ProcessLoginRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.Login r) throws Exception;
    protected abstract long ProcessNormalCloseRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.NormalClose r) throws Exception;
    protected abstract long ProcessReLoginRequest(Zeze.Builtin.GlobalCacheManagerWithRaft.ReLogin r) throws Exception;
}
