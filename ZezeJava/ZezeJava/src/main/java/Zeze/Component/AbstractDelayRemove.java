// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractDelayRemove implements Zeze.IModule {
    public static final int ModuleId = 11007;
    public static final String ModuleName = "DelayRemove";
    public static final String ModuleFullName = "Zeze.Component.DelayRemove";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Zeze.Builtin.DelayRemove.tJobs _tJobs = new Zeze.Builtin.DelayRemove.tJobs();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tJobs.getName()).getDatabaseName(), _tJobs);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tJobs.getName()).getDatabaseName(), _tJobs);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
