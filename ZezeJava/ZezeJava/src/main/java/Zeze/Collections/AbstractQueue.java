// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractQueue implements Zeze.IModule {
    public static final int ModuleId = 11006;
    public static final String ModuleName = "Queue";
    public static final String ModuleFullName = "Zeze.Collections.Queue";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

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
}
