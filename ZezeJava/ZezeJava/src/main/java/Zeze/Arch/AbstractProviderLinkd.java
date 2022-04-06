// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractProviderLinkd extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Beans.Provider"; }
    @Override public String getName() { return "Provider"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11008;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.AnnounceProviderInfo>();
            factoryHandle.Factory = Zeze.Beans.Provider.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfo", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47281670848105L, factoryHandle); // 11008, -1624113559
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Bind>();
            factoryHandle.Factory = Zeze.Beans.Provider.Bind::new;
            factoryHandle.Handle = this::ProcessBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282301515237L, factoryHandle); // 11008, -993446427
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Broadcast>();
            factoryHandle.Factory = Zeze.Beans.Provider.Broadcast::new;
            factoryHandle.Handle = this::ProcessBroadcast;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBroadcast", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282243906435L, factoryHandle); // 11008, -1051055229
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Kick>();
            factoryHandle.Factory = Zeze.Beans.Provider.Kick::new;
            factoryHandle.Handle = this::ProcessKick;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKick", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282516612067L, factoryHandle); // 11008, -778349597
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Send>();
            factoryHandle.Factory = Zeze.Beans.Provider.Send::new;
            factoryHandle.Handle = this::ProcessSend;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSend", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280423652415L, factoryHandle); // 11008, 1423658047
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.SetUserState>();
            factoryHandle.Factory = Zeze.Beans.Provider.SetUserState::new;
            factoryHandle.Handle = this::ProcessSetUserState;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetUserState", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281174282091L, factoryHandle); // 11008, -2120679573
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Subscribe>();
            factoryHandle.Factory = Zeze.Beans.Provider.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282665133980L, factoryHandle); // 11008, -629827684
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.UnBind>();
            factoryHandle.Factory = Zeze.Beans.Provider.UnBind::new;
            factoryHandle.Handle = this::ProcessUnBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280773808911L, factoryHandle); // 11008, 1773814543
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47281670848105L);
        service.getFactorys().remove(47282301515237L);
        service.getFactorys().remove(47282243906435L);
        service.getFactorys().remove(47282516612067L);
        service.getFactorys().remove(47280423652415L);
        service.getFactorys().remove(47281174282091L);
        service.getFactorys().remove(47282665133980L);
        service.getFactorys().remove(47280773808911L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceProviderInfo(Zeze.Beans.Provider.AnnounceProviderInfo p) throws Throwable;
    protected abstract long ProcessBindRequest(Zeze.Beans.Provider.Bind r) throws Throwable;
    protected abstract long ProcessBroadcast(Zeze.Beans.Provider.Broadcast p) throws Throwable;
    protected abstract long ProcessKick(Zeze.Beans.Provider.Kick p) throws Throwable;
    protected abstract long ProcessSend(Zeze.Beans.Provider.Send p) throws Throwable;
    protected abstract long ProcessSetUserState(Zeze.Beans.Provider.SetUserState p) throws Throwable;
    protected abstract long ProcessSubscribeRequest(Zeze.Beans.Provider.Subscribe r) throws Throwable;
    protected abstract long ProcessUnBindRequest(Zeze.Beans.Provider.UnBind r) throws Throwable;
}
