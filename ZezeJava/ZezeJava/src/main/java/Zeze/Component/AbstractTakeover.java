// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractTakeover implements Zeze.IModule {
    public static final int ModuleId = 11045;
    public static final String ModuleName = "Takeover";
    public static final String ModuleFullName = "Zeze.Component.Takeover";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Zeze.Builtin.Takeover.tTakeoverLease _tTakeoverLease = new Zeze.Builtin.Takeover.tTakeoverLease();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tTakeoverLease.getName()).getDatabaseName(), _tTakeoverLease);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tTakeoverLease.getName()).getDatabaseName(), _tTakeoverLease);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
