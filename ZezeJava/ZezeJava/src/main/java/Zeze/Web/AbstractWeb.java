// auto-generated @formatter:off
package Zeze.Web;

public abstract class AbstractWeb extends Zeze.IModule {
    public static final int ModuleId = 11102;
    @Override public String getFullName() { return "Zeze.Web.Web"; }
    @Override public String getName() { return "Web"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Web.tSessions _tSessions = new Zeze.Builtin.Web.tSessions();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.AuthOk>();
            factoryHandle.Factory = Zeze.Builtin.Web.AuthOk::new;
            factoryHandle.Handle = this::ProcessAuthOkRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthOkRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAuthOkRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47682994316792L, factoryHandle); // 11102, 267396600
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestJson>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestJson::new;
            factoryHandle.Handle = this::ProcessRequestJsonRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestJsonRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRequestJsonRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47685215163543L, factoryHandle); // 11102, -1806723945
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Web.RequestQuery>();
            factoryHandle.Factory = Zeze.Builtin.Web.RequestQuery::new;
            factoryHandle.Handle = this::ProcessRequestQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRequestQueryRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRequestQueryRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47686709906514L, factoryHandle); // 11102, -311980974
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47682994316792L);
        service.getFactorys().remove(47685215163543L);
        service.getFactorys().remove(47686709906514L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tSessions.getName()).getDatabaseName(), _tSessions);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tSessions.getName()).getDatabaseName(), _tSessions);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAuthOkRequest(Zeze.Builtin.Web.AuthOk r) throws Throwable;
    protected abstract long ProcessRequestJsonRequest(Zeze.Builtin.Web.RequestJson r) throws Throwable;
    protected abstract long ProcessRequestQueryRequest(Zeze.Builtin.Web.RequestQuery r) throws Throwable;
}
