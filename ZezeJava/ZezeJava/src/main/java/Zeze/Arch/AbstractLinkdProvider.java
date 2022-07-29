// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractLinkdProvider extends Zeze.IModule {
    public static final int ModuleId = 11008;
    @Override public String getFullName() { return "Zeze.Arch.LinkdProvider"; }
    @Override public String getName() { return "LinkdProvider"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int DuplicateExchangeId = 1;
    public static final int UnknownPath404 = 2;
    public static final int ServletException = 3;
    public static final int ExchangeIdNotFound = 4;
    public static final int OnUploadException = 5;

    protected final Zeze.Builtin.Web.tSessions _tSessions = new Zeze.Builtin.Web.tSessions();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.AnnounceProviderInfo>();
            factoryHandle.Factory = Zeze.Builtin.Provider.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfo", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAnnounceProviderInfo", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47279202608226L, factoryHandle); // 11008, 202613858
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Bind>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Bind::new;
            factoryHandle.Handle = this::ProcessBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBindRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47279114253990L, factoryHandle); // 11008, 114259622
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Broadcast>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Broadcast::new;
            factoryHandle.Handle = this::ProcessBroadcast;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBroadcast", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBroadcast", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47282408036866L, factoryHandle); // 11008, -886924798
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Kick>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Kick::new;
            factoryHandle.Handle = this::ProcessKick;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKick", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKick", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47283221887522L, factoryHandle); // 11008, -73074142
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Send>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Send::new;
            factoryHandle.Handle = this::ProcessSend;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSend", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSend", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281226998238L, factoryHandle); // 11008, -2067963426
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.SetUserState>();
            factoryHandle.Factory = Zeze.Builtin.Provider.SetUserState::new;
            factoryHandle.Handle = this::ProcessSetUserState;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetUserState", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetUserState", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281569047175L, factoryHandle); // 11008, -1725914489
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Subscribe>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47280110454586L, factoryHandle); // 11008, 1110460218
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.UnBind>();
            factoryHandle.Factory = Zeze.Builtin.Provider.UnBind::new;
            factoryHandle.Handle = this::ProcessUnBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnBindRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281107578964L, factoryHandle); // 11008, 2107584596
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.AuthOk>();
            factoryHandle.Factory = Zeze.Builtin.Web.AuthOk::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthOkRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAuthOkRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47682994316792L, factoryHandle); // 11102, 267396600
        }
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
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRequestRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47686903989781L, factoryHandle); // 11102, -117897707
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestInputStream>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestInputStream::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestInputStreamRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRequestInputStreamRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47684633737525L, factoryHandle); // 11102, 1906817333
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.ResponseOutputStream>();
            factoryHandle.Factory = Zeze.Builtin.Web.ResponseOutputStream::new;
            factoryHandle.Handle = this::ProcessResponseOutputStreamRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessResponseOutputStreamRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessResponseOutputStreamRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47683493379098L, factoryHandle); // 11102, 766458906
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47279202608226L);
        service.getFactorys().remove(47279114253990L);
        service.getFactorys().remove(47282408036866L);
        service.getFactorys().remove(47283221887522L);
        service.getFactorys().remove(47281226998238L);
        service.getFactorys().remove(47281569047175L);
        service.getFactorys().remove(47280110454586L);
        service.getFactorys().remove(47281107578964L);
        service.getFactorys().remove(47682994316792L);
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

    protected abstract long ProcessAnnounceProviderInfo(Zeze.Builtin.Provider.AnnounceProviderInfo p) throws Throwable;
    protected abstract long ProcessBindRequest(Zeze.Builtin.Provider.Bind r) throws Throwable;
    protected abstract long ProcessBroadcast(Zeze.Builtin.Provider.Broadcast p) throws Throwable;
    protected abstract long ProcessKick(Zeze.Builtin.Provider.Kick p) throws Throwable;
    protected abstract long ProcessSend(Zeze.Builtin.Provider.Send p) throws Throwable;
    protected abstract long ProcessSetUserState(Zeze.Builtin.Provider.SetUserState p) throws Throwable;
    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.Provider.Subscribe r) throws Throwable;
    protected abstract long ProcessUnBindRequest(Zeze.Builtin.Provider.UnBind r) throws Throwable;

    protected abstract long ProcessCloseExchangeRequest(Zeze.Builtin.Web.CloseExchange r) throws Throwable;
    protected abstract long ProcessResponseOutputStreamRequest(Zeze.Builtin.Web.ResponseOutputStream r) throws Throwable;
}
