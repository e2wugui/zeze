// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractProviderDirect extends Zeze.IModule {
    public String getFullName() { return "Zeze.Beans.ProviderDirect"; }
    public String getName() { return "ProviderDirect"; }
    public int getId() { return ModuleId; }
    public static final int ModuleId = 11009;

    public static final int ErrorTransmitParameterFactoryNotFound = 1;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.ProviderDirect.AnnounceProviderInfo>();
            factoryHandle.Factory = Zeze.Beans.ProviderDirect.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfoRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfoRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47283356221296L, factoryHandle); // 11009, 61259632
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.ProviderDirect.ModuleRedirect>();
            factoryHandle.Factory = Zeze.Beans.ProviderDirect.ModuleRedirect::new;
            factoryHandle.Handle = this::ProcessModuleRedirectRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47286708377899L, factoryHandle); // 11009, -881551061
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest>();
            factoryHandle.Factory = Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47286357293504L, factoryHandle); // 11009, -1232635456
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.ProviderDirect.ModuleRedirectAllResult>();
            factoryHandle.Factory = Zeze.Beans.ProviderDirect.ModuleRedirectAllResult::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllResult;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllResult", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47286982651743L, factoryHandle); // 11009, -607277217
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.ProviderDirect.Transmit>();
            factoryHandle.Factory = Zeze.Beans.ProviderDirect.Transmit::new;
            factoryHandle.Handle = this::ProcessTransmit;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTransmit", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47284548257601L, factoryHandle); // 11009, 1253295937
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47283356221296L);
        service.getFactorys().remove(47286708377899L);
        service.getFactorys().remove(47286357293504L);
        service.getFactorys().remove(47286982651743L);
        service.getFactorys().remove(47284548257601L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceProviderInfoRequest(Zeze.Beans.ProviderDirect.AnnounceProviderInfo r) throws Throwable;
    protected abstract long ProcessModuleRedirectRequest(Zeze.Beans.ProviderDirect.ModuleRedirect r) throws Throwable;
    protected abstract long ProcessModuleRedirectAllRequest(Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest p) throws Throwable;
    protected abstract long ProcessModuleRedirectAllResult(Zeze.Beans.ProviderDirect.ModuleRedirectAllResult p) throws Throwable;
    protected abstract long ProcessTransmit(Zeze.Beans.ProviderDirect.Transmit p) throws Throwable;
}
