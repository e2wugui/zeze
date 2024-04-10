// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractDAG implements Zeze.IModule {
    public static final int ModuleId = 11017;
    public static final String ModuleName = "DAG";
    public static final String ModuleFullName = "Zeze.Collections.DAG";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

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
}
