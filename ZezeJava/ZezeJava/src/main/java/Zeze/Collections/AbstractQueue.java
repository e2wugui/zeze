// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractQueue extends Zeze.IModule {
    public static final int ModuleId = 11006;
    @Override public String getFullName() { return "Zeze.Collections.Queue"; }
    @Override public String getName() { return "Queue"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Collections.Queue.tQueueNodes _tQueueNodes = new Zeze.Builtin.Collections.Queue.tQueueNodes();
    protected final Zeze.Builtin.Collections.Queue.tQueues _tQueues = new Zeze.Builtin.Collections.Queue.tQueues();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tQueueNodes.getName()).getDatabaseName(), _tQueueNodes);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tQueues.getName()).getDatabaseName(), _tQueues);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tQueueNodes.getName()).getDatabaseName(), _tQueueNodes);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tQueues.getName()).getDatabaseName(), _tQueues);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
