// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractProviderImplement extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Beans.Provider"; }
    @Override public String getName() { return "Provider"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11008;

    public static final int ErrorTransmitParameterFactoryNotFound = 1;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.AnnounceLinkInfo>();
            factoryHandle.Factory = Zeze.Beans.Provider.AnnounceLinkInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceLinkInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceLinkInfo", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47282968534786L, factoryHandle); // 11008, -326426878
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Bind>();
            factoryHandle.Factory = Zeze.Beans.Provider.Bind::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282301515237L, factoryHandle); // 11008, -993446427
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Dispatch>();
            factoryHandle.Factory = Zeze.Beans.Provider.Dispatch::new;
            factoryHandle.Handle = this::ProcessDispatch;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDispatch", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282067822559L, factoryHandle); // 11008, -1227139105
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.LinkBroken>();
            factoryHandle.Factory = Zeze.Beans.Provider.LinkBroken::new;
            factoryHandle.Handle = this::ProcessLinkBroken;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLinkBroken", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280680546638L, factoryHandle); // 11008, 1680552270
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.SendConfirm>();
            factoryHandle.Factory = Zeze.Beans.Provider.SendConfirm::new;
            factoryHandle.Handle = this::ProcessSendConfirm;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendConfirm", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281317762384L, factoryHandle); // 11008, -1977199280
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Subscribe>();
            factoryHandle.Factory = Zeze.Beans.Provider.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282665133980L, factoryHandle); // 11008, -629827684
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.UnBind>();
            factoryHandle.Factory = Zeze.Beans.Provider.UnBind::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280773808911L, factoryHandle); // 11008, 1773814543
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47282968534786L);
        service.getFactorys().remove(47282301515237L);
        service.getFactorys().remove(47282067822559L);
        service.getFactorys().remove(47280680546638L);
        service.getFactorys().remove(47281317762384L);
        service.getFactorys().remove(47282665133980L);
        service.getFactorys().remove(47280773808911L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceLinkInfo(Zeze.Beans.Provider.AnnounceLinkInfo p) throws Throwable;
    protected abstract long ProcessDispatch(Zeze.Beans.Provider.Dispatch p) throws Throwable;
    protected abstract long ProcessLinkBroken(Zeze.Beans.Provider.LinkBroken p) throws Throwable;
    protected abstract long ProcessSendConfirm(Zeze.Beans.Provider.SendConfirm p) throws Throwable;
}
