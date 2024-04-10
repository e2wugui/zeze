// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractLogAgent implements Zeze.IModule {
    public static final int ModuleId = 11035;
    public static final String ModuleName = "LogAgent";
    public static final String ModuleFullName = "Zeze.Services.LogAgent";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.Browse.class, Zeze.Builtin.LogService.Browse.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.Browse::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBrowseResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBrowseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47397693120348L, factoryHandle); // 11035, -1565958308
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.CloseSession.class, Zeze.Builtin.LogService.CloseSession.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.CloseSession::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseSessionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseSessionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47398269404133L, factoryHandle); // 11035, -989674523
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.NewSession.class, Zeze.Builtin.LogService.NewSession.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.NewSession::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNewSessionResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNewSessionResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47398709984234L, factoryHandle); // 11035, -549094422
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.Query.class, Zeze.Builtin.LogService.Query.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.Query::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47396357146077L, factoryHandle); // 11035, 1393034717
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.Search.class, Zeze.Builtin.LogService.Search.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.Search::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSearchResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSearchResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47395054867890L, factoryHandle); // 11035, 90756530
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47397693120348L);
        service.getFactorys().remove(47398269404133L);
        service.getFactorys().remove(47398709984234L);
        service.getFactorys().remove(47396357146077L);
        service.getFactorys().remove(47395054867890L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
