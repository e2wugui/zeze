// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractLogService implements Zeze.IModule {
    public static final int ModuleId = 11035;
    public static final String ModuleName = "LogService";
    public static final String ModuleFullName = "Zeze.Services.LogService";

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
            factoryHandle.Handle = this::ProcessBrowseRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBrowseRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBrowseRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47397693120348L, factoryHandle); // 11035, -1565958308
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.CloseSession.class, Zeze.Builtin.LogService.CloseSession.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.CloseSession::new;
            factoryHandle.Handle = this::ProcessCloseSessionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseSessionRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseSessionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47398269404133L, factoryHandle); // 11035, -989674523
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.NewSession.class, Zeze.Builtin.LogService.NewSession.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.NewSession::new;
            factoryHandle.Handle = this::ProcessNewSessionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNewSessionRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNewSessionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47398709984234L, factoryHandle); // 11035, -549094422
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.Query.class, Zeze.Builtin.LogService.Query.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.Query::new;
            factoryHandle.Handle = this::ProcessQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47396357146077L, factoryHandle); // 11035, 1393034717
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.Search.class, Zeze.Builtin.LogService.Search.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.Search::new;
            factoryHandle.Handle = this::ProcessSearchRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSearchRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSearchRequest", Zeze.Transaction.DispatchMode.Normal);
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

    protected abstract long ProcessBrowseRequest(Zeze.Builtin.LogService.Browse r) throws Exception;
    protected abstract long ProcessCloseSessionRequest(Zeze.Builtin.LogService.CloseSession r) throws Exception;
    protected abstract long ProcessNewSessionRequest(Zeze.Builtin.LogService.NewSession r) throws Exception;
    protected abstract long ProcessQueryRequest(Zeze.Builtin.LogService.Query r) throws Exception;
    protected abstract long ProcessSearchRequest(Zeze.Builtin.LogService.Search r) throws Exception;
}
