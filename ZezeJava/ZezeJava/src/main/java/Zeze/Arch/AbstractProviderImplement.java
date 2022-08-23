// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractProviderImplement extends Zeze.IModule {
    public static final int ModuleId = 11008;
    @Override public String getFullName() { return "Zeze.Arch.ProviderImplement"; }
    @Override public String getName() { return "ProviderImplement"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.AnnounceLinkInfo>();
            factoryHandle.Factory = Zeze.Builtin.Provider.AnnounceLinkInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceLinkInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceLinkInfo", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAnnounceLinkInfo", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281374674071L, factoryHandle); // 11008, -1920287593
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Bind>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Bind::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBindRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47279114253990L, factoryHandle); // 11008, 114259622
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Dispatch>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Dispatch::new;
            factoryHandle.Handle = this::ProcessDispatch;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDispatch", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessDispatch", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47280285301785L, factoryHandle); // 11008, 1285307417
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.LinkBroken>();
            factoryHandle.Factory = Zeze.Builtin.Provider.LinkBroken::new;
            factoryHandle.Handle = this::ProcessLinkBroken;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLinkBroken", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLinkBroken", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281652939086L, factoryHandle); // 11008, -1642022578
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Send>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Send::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281226998238L, factoryHandle); // 11008, -2067963426
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.Subscribe>();
            factoryHandle.Factory = Zeze.Builtin.Provider.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47280110454586L, factoryHandle); // 11008, 1110460218
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Provider.UnBind>();
            factoryHandle.Factory = Zeze.Builtin.Provider.UnBind::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnBindRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281107578964L, factoryHandle); // 11008, 2107584596
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47281374674071L);
        service.getFactorys().remove(47279114253990L);
        service.getFactorys().remove(47280285301785L);
        service.getFactorys().remove(47281652939086L);
        service.getFactorys().remove(47281226998238L);
        service.getFactorys().remove(47280110454586L);
        service.getFactorys().remove(47281107578964L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceLinkInfo(Zeze.Builtin.Provider.AnnounceLinkInfo p) throws Throwable;
    protected abstract long ProcessDispatch(Zeze.Builtin.Provider.Dispatch p) throws Throwable;
    protected abstract long ProcessLinkBroken(Zeze.Builtin.Provider.LinkBroken p) throws Throwable;
}
