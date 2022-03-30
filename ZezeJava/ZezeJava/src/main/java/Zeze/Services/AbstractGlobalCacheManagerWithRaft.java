// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractGlobalCacheManagerWithRaft {
    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Acquire>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Acquire::new;
            factoryHandle.Handle = this::ProcessAcquireRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcquireRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251758877516L, factoryHandle); // 11001, -1471313076
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup::new;
            factoryHandle.Handle = this::ProcessCleanupRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCleanupRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47249689802603L, factoryHandle); // 11001, 754579307
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive::new;
            factoryHandle.Handle = this::ProcessKeepAliveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47250139303472L, factoryHandle); // 11001, 1204080176
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Login>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251605578232L, factoryHandle); // 11001, -1624612360
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose::new;
            factoryHandle.Handle = this::ProcessNormalCloseRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47250988461421L, factoryHandle); // 11001, 2053238125
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Reduce>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Reduce::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47252602373450L, factoryHandle); // 11001, -627817142
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin::new;
            factoryHandle.Handle = this::ProcessReLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251661990773L, factoryHandle); // 11001, -1568199819
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47251758877516L);
        service.getFactorys().remove(47249689802603L);
        service.getFactorys().remove(47250139303472L);
        service.getFactorys().remove(47251605578232L);
        service.getFactorys().remove(47250988461421L);
        service.getFactorys().remove(47252602373450L);
        service.getFactorys().remove(47251661990773L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.RegisterTableTemplate("Global", Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey.class, Zeze.Beans.GlobalCacheManagerWithRaft.CacheState.class);
        rocks.RegisterTableTemplate("Session", Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey.class, Zeze.Beans.GlobalCacheManagerWithRaft.AcquiredState.class);
        Zeze.Raft.RocksRaft.Rocks.RegisterLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(Integer.class));
    }

    protected abstract long ProcessAcquireRequest(Zeze.Beans.GlobalCacheManagerWithRaft.Acquire r) throws Throwable;
    protected abstract long ProcessCleanupRequest(Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup r) throws Throwable;
    protected abstract long ProcessKeepAliveRequest(Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive r) throws Throwable;
    protected abstract long ProcessLoginRequest(Zeze.Beans.GlobalCacheManagerWithRaft.Login r) throws Throwable;
    protected abstract long ProcessNormalCloseRequest(Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose r) throws Throwable;
    protected abstract long ProcessReLoginRequest(Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin r) throws Throwable;
}
