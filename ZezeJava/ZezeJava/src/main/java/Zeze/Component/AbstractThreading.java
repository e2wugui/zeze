// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractThreading implements Zeze.IModule {
    public static final int ModuleId = 11030;
    public static final String ModuleName = "Threading";
    public static final String ModuleFullName = "Zeze.Component.Threading";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Threading.tMutex _tMutex = new Zeze.Builtin.Threading.tMutex();
    protected final Zeze.Builtin.Threading.tReadWriteLock _tReadWriteLock = new Zeze.Builtin.Threading.tReadWriteLock();
    protected final Zeze.Builtin.Threading.tSemaphore _tSemaphore = new Zeze.Builtin.Threading.tSemaphore();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tMutex.getName()).getDatabaseName(), _tMutex);
        zeze.addTable(zeze.getConfig().getTableConf(_tReadWriteLock.getName()).getDatabaseName(), _tReadWriteLock);
        zeze.addTable(zeze.getConfig().getTableConf(_tSemaphore.getName()).getDatabaseName(), _tSemaphore);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tMutex.getName()).getDatabaseName(), _tMutex);
        zeze.removeTable(zeze.getConfig().getTableConf(_tReadWriteLock.getName()).getDatabaseName(), _tReadWriteLock);
        zeze.removeTable(zeze.getConfig().getTableConf(_tSemaphore.getName()).getDatabaseName(), _tSemaphore);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
