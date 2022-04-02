// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractGlobalCacheManagerWithRaftAgent extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Beans.GlobalCacheManagerWithRaft"; }
    @Override public String getName() { return "GlobalCacheManagerWithRaft"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11001;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Acquire>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Acquire::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcquireRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251758877516L, factoryHandle); // 11001, -1471313076
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Cleanup::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCleanupRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47249689802603L, factoryHandle); // 11001, 754579307
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.KeepAlive::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47250139303472L, factoryHandle); // 11001, 1204080176
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Login>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Login::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47251605578232L, factoryHandle); // 11001, -1624612360
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.NormalClose::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47250988461421L, factoryHandle); // 11001, 2053238125
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.Reduce>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.Reduce::new;
            factoryHandle.Handle = this::ProcessReduceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReduceRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47252602373450L, factoryHandle); // 11001, -627817142
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin>();
            factoryHandle.Factory = Zeze.Beans.GlobalCacheManagerWithRaft.ReLogin::new;
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
    }

    protected abstract long ProcessReduceRequest(Zeze.Beans.GlobalCacheManagerWithRaft.Reduce r) throws Throwable;
}
