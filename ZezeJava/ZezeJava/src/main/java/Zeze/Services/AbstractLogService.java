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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.NewSessionRegex.class, Zeze.Builtin.LogService.NewSessionRegex.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.NewSessionRegex::new;
            factoryHandle.Handle = this::ProcessNewSessionRegexRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNewSessionRegexRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNewSessionRegexRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47395876632588L, factoryHandle); // 11035, 912521228
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.NewSessionWords.class, Zeze.Builtin.LogService.NewSessionWords.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.NewSessionWords::new;
            factoryHandle.Handle = this::ProcessNewSessionWordsRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNewSessionWordsRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNewSessionWordsRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47397729064466L, factoryHandle); // 11035, -1530014190
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
        service.getFactorys().remove(47395876632588L);
        service.getFactorys().remove(47397729064466L);
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
    protected abstract long ProcessNewSessionRegexRequest(Zeze.Builtin.LogService.NewSessionRegex r) throws Exception;
    protected abstract long ProcessNewSessionWordsRequest(Zeze.Builtin.LogService.NewSessionWords r) throws Exception;
    protected abstract long ProcessSearchRequest(Zeze.Builtin.LogService.Search r) throws Exception;
}
