// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractLoginQueueServer implements Zeze.IModule {
    public static final int ModuleId = 11042;
    public static final String ModuleName = "LoginQueueServer";
    public static final String ModuleFullName = "Zeze.Services.LoginQueueServer";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LoginQueueServer.ReportProviderLoad.class, Zeze.Builtin.LoginQueueServer.ReportProviderLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LoginQueueServer.ReportProviderLoad::new;
            factoryHandle.Handle = this::ProcessReportProviderLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportProviderLoadRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportProviderLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47426009765783L, factoryHandle); // 11042, 980883351
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LoginQueueServer.ReportLinkLoad.class, Zeze.Builtin.LoginQueueServer.ReportLinkLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LoginQueueServer.ReportLinkLoad::new;
            factoryHandle.Handle = this::ProcessReportLinkLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLinkLoadRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLinkLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47427598889391L, factoryHandle); // 11042, -1724960337
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LoginQueueServer.AnnounceSecret.class, Zeze.Builtin.LoginQueueServer.AnnounceSecret.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LoginQueueServer.AnnounceSecret::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceSecretResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAnnounceSecretResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47425858531401L, factoryHandle); // 11042, 829648969
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47426009765783L);
        service.getFactorys().remove(47427598889391L);
        service.getFactorys().remove(47425858531401L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessReportProviderLoadRequest(Zeze.Builtin.LoginQueueServer.ReportProviderLoad r) throws Exception;
    protected abstract long ProcessReportLinkLoadRequest(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) throws Exception;
}
