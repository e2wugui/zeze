// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractDbWeb extends Zeze.IModule {
    public static final int ModuleId = 11021;
    @Override public String getFullName() { return "Zeze.Component.DbWeb"; }
    @Override public String getName() { return "DbWeb"; }
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
        httpServer.addHandler("/Zeze/Builtin/DbWeb/GetValue", 8192,
                _reflect.getTransactionLevel("OnServletGetValue", Zeze.Transaction.TransactionLevel.None),
                _reflect.getDispatchMode("OnServletGetValue", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletGetValue);
        httpServer.addHandler("/Zeze/Builtin/DbWeb/ListTable", 8192,
                _reflect.getTransactionLevel("OnServletListTable", Zeze.Transaction.TransactionLevel.None),
                _reflect.getDispatchMode("OnServletListTable", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletListTable);
        httpServer.addHandler("/Zeze/Builtin/DbWeb/WalkTable", 8192,
                _reflect.getTransactionLevel("OnServletWalkTable", Zeze.Transaction.TransactionLevel.None),
                _reflect.getDispatchMode("OnServletWalkTable", Zeze.Transaction.DispatchMode.Normal),
                this::OnServletWalkTable);
    }

    protected abstract void OnServletGetValue(Zeze.Netty.HttpExchange x) throws Exception;
    protected abstract void OnServletListTable(Zeze.Netty.HttpExchange x) throws Exception;
    protected abstract void OnServletWalkTable(Zeze.Netty.HttpExchange x) throws Exception;
}
