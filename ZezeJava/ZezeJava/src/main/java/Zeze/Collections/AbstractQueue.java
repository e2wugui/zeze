// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractQueue extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Builtin.Collections.Queue"; }
    @Override public String getName() { return "Queue"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11006;

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
