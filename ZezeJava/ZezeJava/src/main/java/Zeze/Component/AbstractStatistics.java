// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractStatistics extends Zeze.IModule {
    public static final int ModuleId = 11020;
    @Override public String getFullName() { return "Zeze.Component.Statistics"; }
    @Override public String getName() { return "Statistics"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        httpServer.addHandler("/Zeze/Builtin/Statistics/Query", 8192,
                _reflect.getTransactionLevel("OnServletQuery", Zeze.Transaction.TransactionLevel.Serializable),
                _reflect.getDispatchMode("OnServletQuery", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletQuery);
    }

    protected abstract void OnServletQuery(Zeze.Netty.HttpExchange x) throws Exception;
}
