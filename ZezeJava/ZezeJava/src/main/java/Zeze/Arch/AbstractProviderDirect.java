// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractProviderDirect extends Zeze.IModule {
    public static final int ModuleId = 11009;
    @Override public String getFullName() { return "Zeze.Arch.ProviderDirect"; }
    @Override public String getName() { return "ProviderDirect"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int ErrorTransmitParameterFactoryNotFound = 1;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ProviderDirect.AnnounceProviderInfo>();
            factoryHandle.Factory = Zeze.Builtin.ProviderDirect.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfoRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfoRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47286041114986L, factoryHandle); // 11009, -1548813974
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ProviderDirect.ModuleRedirect>();
            factoryHandle.Factory = Zeze.Builtin.ProviderDirect.ModuleRedirect::new;
            factoryHandle.Handle = this::ProcessModuleRedirectRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47284402955566L, factoryHandle); // 11009, 1107993902
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest>();
            factoryHandle.Factory = Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47286816262188L, factoryHandle); // 11009, -773666772
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult>();
            factoryHandle.Factory = Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllResult;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllResult", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47283400371444L, factoryHandle); // 11009, 105409780
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ProviderDirect.Transmit>();
            factoryHandle.Factory = Zeze.Builtin.ProviderDirect.Transmit::new;
            factoryHandle.Handle = this::ProcessTransmit;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTransmit", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47284197108752L, factoryHandle); // 11009, 902147088
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ProviderDirect.TransmitAccount>();
            factoryHandle.Factory = Zeze.Builtin.ProviderDirect.TransmitAccount::new;
            factoryHandle.Handle = this::ProcessTransmitAccount;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTransmitAccount", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47284247217006L, factoryHandle); // 11009, 952255342
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47286041114986L);
        service.getFactorys().remove(47284402955566L);
        service.getFactorys().remove(47286816262188L);
        service.getFactorys().remove(47283400371444L);
        service.getFactorys().remove(47284197108752L);
        service.getFactorys().remove(47284247217006L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceProviderInfoRequest(Zeze.Builtin.ProviderDirect.AnnounceProviderInfo r) throws Throwable;
    protected abstract long ProcessModuleRedirectRequest(Zeze.Builtin.ProviderDirect.ModuleRedirect r) throws Throwable;
    protected abstract long ProcessModuleRedirectAllRequest(Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest p) throws Throwable;
    protected abstract long ProcessModuleRedirectAllResult(Zeze.Builtin.ProviderDirect.ModuleRedirectAllResult p) throws Throwable;
    protected abstract long ProcessTransmit(Zeze.Builtin.ProviderDirect.Transmit p) throws Throwable;
    protected abstract long ProcessTransmitAccount(Zeze.Builtin.ProviderDirect.TransmitAccount p) throws Throwable;
}
