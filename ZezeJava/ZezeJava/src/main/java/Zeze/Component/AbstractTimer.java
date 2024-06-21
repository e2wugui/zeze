// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractTimer implements Zeze.IModule {
    public static final int ModuleId = 11016;
    public static final String ModuleName = "Timer";
    public static final String ModuleFullName = "Zeze.Component.Timer";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eMissfirePolicyNothing = 0; // 如果错过指定触发时间, 则忽略并以当前时间+period重设新的触发时间
    public static final int eMissfirePolicyRunOnce = 1; // 如果错过指定触发时间, 则立即触发一次并以当前时间+period重设新的触发时间
    public static final int eMissfirePolicyRunOnceOldNext = 2; // 如果错过指定触发时间, 则立即触发一次并以之前触发时间+period重设新的触发时间, 用于定点时间触发

    protected final Zeze.Builtin.Timer.tAccountOfflineTimers _tAccountOfflineTimers = new Zeze.Builtin.Timer.tAccountOfflineTimers();
    protected final Zeze.Builtin.Timer.tAccountTimers _tAccountTimers = new Zeze.Builtin.Timer.tAccountTimers();
    protected final Zeze.Builtin.Timer.tIndexs _tIndexs = new Zeze.Builtin.Timer.tIndexs();
    protected final Zeze.Builtin.Timer.tNodeRoot _tNodeRoot = new Zeze.Builtin.Timer.tNodeRoot();
    protected final Zeze.Builtin.Timer.tNodes _tNodes = new Zeze.Builtin.Timer.tNodes();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tAccountOfflineTimers.getName()).getDatabaseName(), _tAccountOfflineTimers);
        zeze.addTable(zeze.getConfig().getTableConf(_tAccountTimers.getName()).getDatabaseName(), _tAccountTimers);
        zeze.addTable(zeze.getConfig().getTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.addTable(zeze.getConfig().getTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.addTable(zeze.getConfig().getTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tAccountOfflineTimers.getName()).getDatabaseName(), _tAccountOfflineTimers);
        zeze.removeTable(zeze.getConfig().getTableConf(_tAccountTimers.getName()).getDatabaseName(), _tAccountTimers);
        zeze.removeTable(zeze.getConfig().getTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
