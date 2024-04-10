// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractLinkdProvider implements Zeze.IModule {
    public static final int ModuleId = 11008;
    public static final String ModuleName = "LinkdProvider";
    public static final String ModuleFullName = "Zeze.Arch.LinkdProvider";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.AnnounceProviderInfo.class, Zeze.Builtin.Provider.AnnounceProviderInfo.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfo", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAnnounceProviderInfo", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47279202608226L, factoryHandle); // 11008, 202613858
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.Bind.class, Zeze.Builtin.Provider.Bind.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.Bind::new;
            factoryHandle.Handle = this::ProcessBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBindRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47279114253990L, factoryHandle); // 11008, 114259622
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.Broadcast.class, Zeze.Builtin.Provider.Broadcast.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.Broadcast::new;
            factoryHandle.Handle = this::ProcessBroadcast;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBroadcast", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBroadcast", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47282408036866L, factoryHandle); // 11008, -886924798
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.CheckLinkSession.class, Zeze.Builtin.Provider.CheckLinkSession.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.CheckLinkSession::new;
            factoryHandle.Handle = this::ProcessCheckLinkSession;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCheckLinkSession", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCheckLinkSession", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47279498073324L, factoryHandle); // 11008, 498078956
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.Kick.class, Zeze.Builtin.Provider.Kick.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.Kick::new;
            factoryHandle.Handle = this::ProcessKick;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKick", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKick", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47283221887522L, factoryHandle); // 11008, -73074142
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.Send.class, Zeze.Builtin.Provider.Send.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.Send::new;
            factoryHandle.Handle = this::ProcessSendRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281226998238L, factoryHandle); // 11008, -2067963426
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.SetDisableChoice.class, Zeze.Builtin.Provider.SetDisableChoice.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.SetDisableChoice::new;
            factoryHandle.Handle = this::ProcessSetDisableChoiceRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetDisableChoiceRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetDisableChoiceRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281262305779L, factoryHandle); // 11008, -2032655885
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.SetUserState.class, Zeze.Builtin.Provider.SetUserState.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.SetUserState::new;
            factoryHandle.Handle = this::ProcessSetUserState;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetUserState", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetUserState", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281569047175L, factoryHandle); // 11008, -1725914489
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.Subscribe.class, Zeze.Builtin.Provider.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47280110454586L, factoryHandle); // 11008, 1110460218
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Provider.UnBind.class, Zeze.Builtin.Provider.UnBind.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Provider.UnBind::new;
            factoryHandle.Handle = this::ProcessUnBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnBindRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47281107578964L, factoryHandle); // 11008, 2107584596
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47279202608226L);
        service.getFactorys().remove(47279114253990L);
        service.getFactorys().remove(47282408036866L);
        service.getFactorys().remove(47279498073324L);
        service.getFactorys().remove(47283221887522L);
        service.getFactorys().remove(47281226998238L);
        service.getFactorys().remove(47281262305779L);
        service.getFactorys().remove(47281569047175L);
        service.getFactorys().remove(47280110454586L);
        service.getFactorys().remove(47281107578964L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceProviderInfo(Zeze.Builtin.Provider.AnnounceProviderInfo p) throws Exception;
    protected abstract long ProcessBindRequest(Zeze.Builtin.Provider.Bind r) throws Exception;
    protected abstract long ProcessBroadcast(Zeze.Builtin.Provider.Broadcast p) throws Exception;
    protected abstract long ProcessCheckLinkSession(Zeze.Builtin.Provider.CheckLinkSession p) throws Exception;
    protected abstract long ProcessKick(Zeze.Builtin.Provider.Kick p) throws Exception;
    protected abstract long ProcessSendRequest(Zeze.Builtin.Provider.Send r) throws Exception;
    protected abstract long ProcessSetDisableChoiceRequest(Zeze.Builtin.Provider.SetDisableChoice r) throws Exception;
    protected abstract long ProcessSetUserState(Zeze.Builtin.Provider.SetUserState p) throws Exception;
    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.Provider.Subscribe r) throws Exception;
    protected abstract long ProcessUnBindRequest(Zeze.Builtin.Provider.UnBind r) throws Exception;
}
