// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractDAG extends Zeze.IModule {
    public static final int ModuleId = 11017;
    @Override public String getFullName() { return "Zeze.Collections.DAG"; }
    @Override public String getName() { return "DAG"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Collections.DAG.tDAGs _tDAGs = new Zeze.Builtin.Collections.DAG.tDAGs();
    protected final Zeze.Builtin.Collections.DAG.tEdge _tEdge = new Zeze.Builtin.Collections.DAG.tEdge();
    protected final Zeze.Builtin.Collections.DAG.tNode _tNode = new Zeze.Builtin.Collections.DAG.tNode();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tDAGs.getName()).getDatabaseName(), _tDAGs);
        zeze.addTable(zeze.getConfig().getTableConf(_tEdge.getName()).getDatabaseName(), _tEdge);
        zeze.addTable(zeze.getConfig().getTableConf(_tNode.getName()).getDatabaseName(), _tNode);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tDAGs.getName()).getDatabaseName(), _tDAGs);
        zeze.removeTable(zeze.getConfig().getTableConf(_tEdge.getName()).getDatabaseName(), _tEdge);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNode.getName()).getDatabaseName(), _tNode);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
