// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractSafeBatch implements Zeze.IModule {
    public static final int ModuleId = 11044;
    public static final String ModuleName = "SafeBatch";
    public static final String ModuleFullName = "Zeze.Component.SafeBatch";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Zeze.Builtin.SafeBatch.tSafeBatchSortedMap _tSafeBatchSortedMap = new Zeze.Builtin.SafeBatch.tSafeBatchSortedMap();
    protected final Zeze.Builtin.SafeBatch.tSafeBatchTable _tSafeBatchTable = new Zeze.Builtin.SafeBatch.tSafeBatchTable();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tSafeBatchSortedMap.getName()).getDatabaseName(), _tSafeBatchSortedMap);
        zeze.addTable(zeze.getConfig().getTableConf(_tSafeBatchTable.getName()).getDatabaseName(), _tSafeBatchTable);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tSafeBatchSortedMap.getName()).getDatabaseName(), _tSafeBatchSortedMap);
        zeze.removeTable(zeze.getConfig().getTableConf(_tSafeBatchTable.getName()).getDatabaseName(), _tSafeBatchTable);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
