// auto-generated @formatter:off
package Zeze.Web;

public abstract class AbstractWeb extends Zeze.IModule {
    public static final int ModuleId = 11102;
    @Override public String getFullName() { return "Zeze.Web.Web"; }
    @Override public String getName() { return "Web"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.AuthJson>();
            factoryHandle.Factory = Zeze.Builtin.Web.AuthJson::new;
            factoryHandle.Handle = this::ProcessAuthJsonRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthJsonRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47684610866360L, factoryHandle); // 11102, 1883946168
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.AuthOk>();
            factoryHandle.Factory = Zeze.Builtin.Web.AuthOk::new;
            factoryHandle.Handle = this::ProcessAuthOkRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthOkRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47682994316792L, factoryHandle); // 11102, 267396600
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.AuthQuery>();
            factoryHandle.Factory = Zeze.Builtin.Web.AuthQuery::new;
            factoryHandle.Handle = this::ProcessAuthQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthQueryRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47684129009271L, factoryHandle); // 11102, 1402089079
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestJson>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestJson::new;
            factoryHandle.Handle = this::ProcessRequestJsonRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestJsonRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47685215163543L, factoryHandle); // 11102, -1806723945
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestQuery>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestQuery::new;
            factoryHandle.Handle = this::ProcessRequestQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestQueryRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47686709906514L, factoryHandle); // 11102, -311980974
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47684610866360L);
        service.getFactorys().remove(47682994316792L);
        service.getFactorys().remove(47684129009271L);
        service.getFactorys().remove(47685215163543L);
        service.getFactorys().remove(47686709906514L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAuthJsonRequest(Zeze.Builtin.Web.AuthJson r) throws Throwable;
    protected abstract long ProcessAuthOkRequest(Zeze.Builtin.Web.AuthOk r) throws Throwable;
    protected abstract long ProcessAuthQueryRequest(Zeze.Builtin.Web.AuthQuery r) throws Throwable;
    protected abstract long ProcessRequestJsonRequest(Zeze.Builtin.Web.RequestJson r) throws Throwable;
    protected abstract long ProcessRequestQueryRequest(Zeze.Builtin.Web.RequestQuery r) throws Throwable;
}
