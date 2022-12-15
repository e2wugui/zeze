// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractQueue implements Zeze.IModule {
    public static final int ModuleId = 11006;
    @Override public String getFullName() { return "Zeze.Collections.Queue"; }
    @Override public String getName() { return "Queue"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Collections.Queue.tQueueNodes _tQueueNodes = new Zeze.Builtin.Collections.Queue.tQueueNodes();
    protected final Zeze.Builtin.Collections.Queue.tQueues _tQueues = new Zeze.Builtin.Collections.Queue.tQueues();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tQueueNodes.getName()).getDatabaseName(), _tQueueNodes);
        zeze.addTable(zeze.getConfig().getTableConf(_tQueues.getName()).getDatabaseName(), _tQueues);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tQueueNodes.getName()).getDatabaseName(), _tQueueNodes);
        zeze.removeTable(zeze.getConfig().getTableConf(_tQueues.getName()).getDatabaseName(), _tQueues);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
