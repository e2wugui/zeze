// auto-generated @formatter:off
package Zeze.Web;

public abstract class AbstractWeb extends Zeze.IModule {
    public static final int ModuleId = 11102;
    @Override public String getFullName() { return "Zeze.Web.Web"; }
    @Override public String getName() { return "Web"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int DuplicateExchangeId = 1;
    public static final int UnknownPath404 = 2;
    public static final int ServletException = 3;
    public static final int ExchangeIdNotFound = 4;
    public static final int OnUploadException = 5;
    public static final int OnDownloadException = 5;

    protected final Zeze.Builtin.Web.tSessions _tSessions = new Zeze.Builtin.Web.tSessions();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.CloseExchange>();
            factoryHandle.Factory = Zeze.Builtin.Web.CloseExchange::new;
            factoryHandle.Handle = this::ProcessCloseExchangeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCloseExchangeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCloseExchangeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47683263889294L, factoryHandle); // 11102, 536969102
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.Request>();
            factoryHandle.Factory = Zeze.Builtin.Web.Request::new;
            factoryHandle.Handle = this::ProcessRequestRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRequestRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47686903989781L, factoryHandle); // 11102, -117897707
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestInputStream>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestInputStream::new;
            factoryHandle.Handle = this::ProcessRequestInputStreamRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestInputStreamRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRequestInputStreamRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47684633737525L, factoryHandle); // 11102, 1906817333
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.ResponseOutputStream>();
            factoryHandle.Factory = Zeze.Builtin.Web.ResponseOutputStream::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessResponseOutputStreamResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessResponseOutputStreamResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47683493379098L, factoryHandle); // 11102, 766458906
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47683263889294L);
        service.getFactorys().remove(47686903989781L);
        service.getFactorys().remove(47684633737525L);
        service.getFactorys().remove(47683493379098L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tSessions.getName()).getDatabaseName(), _tSessions);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tSessions.getName()).getDatabaseName(), _tSessions);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCloseExchangeRequest(Zeze.Builtin.Web.CloseExchange r) throws Throwable;
    protected abstract long ProcessRequestRequest(Zeze.Builtin.Web.Request r) throws Throwable;
    protected abstract long ProcessRequestInputStreamRequest(Zeze.Builtin.Web.RequestInputStream r) throws Throwable;
}
