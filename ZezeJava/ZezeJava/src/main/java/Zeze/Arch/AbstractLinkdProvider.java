// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractLinkdProvider extends Zeze.IModule {
    public static final int ModuleId = 11008;
    @Override public String getFullName() { return "Zeze.Arch.LinkdProvider"; }
    @Override public String getName() { return "LinkdProvider"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.AnnounceProviderInfo>();
            factoryHandle.Factory = Zeze.Builtin.Provider.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfo", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47279202608226L, factoryHandle); // 11008, 202613858
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Bind>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Bind::new;
            factoryHandle.Handle = this::ProcessBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47279114253990L, factoryHandle); // 11008, 114259622
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Broadcast>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Broadcast::new;
            factoryHandle.Handle = this::ProcessBroadcast;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBroadcast", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282408036866L, factoryHandle); // 11008, -886924798
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Kick>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Kick::new;
            factoryHandle.Handle = this::ProcessKick;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKick", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47283221887522L, factoryHandle); // 11008, -73074142
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Send>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Send::new;
            factoryHandle.Handle = this::ProcessSend;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSend", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281226998238L, factoryHandle); // 11008, -2067963426
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.SetUserState>();
            factoryHandle.Factory = Zeze.Builtin.Provider.SetUserState::new;
            factoryHandle.Handle = this::ProcessSetUserState;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetUserState", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281569047175L, factoryHandle); // 11008, -1725914489
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Subscribe>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280110454586L, factoryHandle); // 11008, 1110460218
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.UnBind>();
            factoryHandle.Factory = Zeze.Builtin.Provider.UnBind::new;
            factoryHandle.Handle = this::ProcessUnBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281107578964L, factoryHandle); // 11008, 2107584596
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestJson>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestJson::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestJsonRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47685215163543L, factoryHandle); // 11102, -1806723945
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestQuery>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestQuery::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestQueryRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47686709906514L, factoryHandle); // 11102, -311980974
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47279202608226L);
        service.getFactorys().remove(47279114253990L);
        service.getFactorys().remove(47282408036866L);
        service.getFactorys().remove(47283221887522L);
        service.getFactorys().remove(47281226998238L);
        service.getFactorys().remove(47281569047175L);
        service.getFactorys().remove(47280110454586L);
        service.getFactorys().remove(47281107578964L);
        service.getFactorys().remove(47685215163543L);
        service.getFactorys().remove(47686709906514L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceProviderInfo(Zeze.Builtin.Provider.AnnounceProviderInfo p) throws Throwable;
    protected abstract long ProcessBindRequest(Zeze.Builtin.Provider.Bind r) throws Throwable;
    protected abstract long ProcessBroadcast(Zeze.Builtin.Provider.Broadcast p) throws Throwable;
    protected abstract long ProcessKick(Zeze.Builtin.Provider.Kick p) throws Throwable;
    protected abstract long ProcessSend(Zeze.Builtin.Provider.Send p) throws Throwable;
    protected abstract long ProcessSetUserState(Zeze.Builtin.Provider.SetUserState p) throws Throwable;
    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.Provider.Subscribe r) throws Throwable;
    protected abstract long ProcessUnBindRequest(Zeze.Builtin.Provider.UnBind r) throws Throwable;
}
