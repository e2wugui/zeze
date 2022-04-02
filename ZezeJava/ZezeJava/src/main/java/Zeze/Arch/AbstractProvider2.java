// auto-generated @formatter:off
package Zeze.Arch;

public abstract class AbstractProvider2 extends Zeze.IModule {
    public String getFullName() { return "Zeze.Beans.Provider2"; }
    public String getName() { return "Provider2"; }
    public int getId() { return ModuleId; }
    public static final int ModuleId = 11009;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider2.ModuleRedirect>();
            factoryHandle.Factory = Zeze.Beans.Provider2.ModuleRedirect::new;
            factoryHandle.Handle = this::ProcessModuleRedirectRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47287474465813L, factoryHandle); // 11009, -115463147
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider2.ModuleRedirectAllRequest>();
            factoryHandle.Factory = Zeze.Beans.Provider2.ModuleRedirectAllRequest::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47283400527630L, factoryHandle); // 11009, 105565966
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider2.ModuleRedirectAllResult>();
            factoryHandle.Factory = Zeze.Beans.Provider2.ModuleRedirectAllResult::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllResult;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllResult", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47285102343394L, factoryHandle); // 11009, 1807381730
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider2.Transmit>();
            factoryHandle.Factory = Zeze.Beans.Provider2.Transmit::new;
            factoryHandle.Handle = this::ProcessTransmit;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTransmit", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47286561407261L, factoryHandle); // 11009, -1028521699
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47287474465813L);
        service.getFactorys().remove(47283400527630L);
        service.getFactorys().remove(47285102343394L);
        service.getFactorys().remove(47286561407261L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessModuleRedirectRequest(Zeze.Beans.Provider2.ModuleRedirect r) throws Throwable;
    protected abstract long ProcessModuleRedirectAllRequest(Zeze.Beans.Provider2.ModuleRedirectAllRequest p) throws Throwable;
    protected abstract long ProcessModuleRedirectAllResult(Zeze.Beans.Provider2.ModuleRedirectAllResult p) throws Throwable;
    protected abstract long ProcessTransmit(Zeze.Beans.Provider2.Transmit p) throws Throwable;
}
