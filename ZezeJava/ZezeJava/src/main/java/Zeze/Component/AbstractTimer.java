// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractTimer extends Zeze.IModule {
    public static final int ModuleId = 11016;
    @Override public String getFullName() { return "Zeze.Component.Timer"; }
    @Override public String getName() { return "Timer"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int eMissfirePolicyNothing = 0; // quartz 兼容
    public static final int eMissfirePolicyRunOnce = 1; // quartz 兼容
    public static final int eMissfirePolicyRunOnceOldNext = 2; // 新策略，马上补一次调用，但保持下一次调度时间不变。比如用于，每天定点开启活动。

    protected final Zeze.Builtin.Timer.tAccountOfflineTimers _tAccountOfflineTimers = new Zeze.Builtin.Timer.tAccountOfflineTimers();
    protected final Zeze.Builtin.Timer.tAccountTimers _tAccountTimers = new Zeze.Builtin.Timer.tAccountTimers();
    protected final Zeze.Builtin.Timer.tCustomClasses _tCustomClasses = new Zeze.Builtin.Timer.tCustomClasses();
    protected final Zeze.Builtin.Timer.tIndexs _tIndexs = new Zeze.Builtin.Timer.tIndexs();
    protected final Zeze.Builtin.Timer.tNodeRoot _tNodeRoot = new Zeze.Builtin.Timer.tNodeRoot();
    protected final Zeze.Builtin.Timer.tNodes _tNodes = new Zeze.Builtin.Timer.tNodes();
    protected final Zeze.Builtin.Timer.tRoleOfflineTimers _tRoleOfflineTimers = new Zeze.Builtin.Timer.tRoleOfflineTimers();
    protected final Zeze.Builtin.Timer.tRoleTimers _tRoleTimers = new Zeze.Builtin.Timer.tRoleTimers();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tAccountOfflineTimers.getName()).getDatabaseName(), _tAccountOfflineTimers);
        zeze.addTable(zeze.getConfig().getTableConf(_tAccountTimers.getName()).getDatabaseName(), _tAccountTimers);
        zeze.addTable(zeze.getConfig().getTableConf(_tCustomClasses.getName()).getDatabaseName(), _tCustomClasses);
        zeze.addTable(zeze.getConfig().getTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.addTable(zeze.getConfig().getTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.addTable(zeze.getConfig().getTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
        zeze.addTable(zeze.getConfig().getTableConf(_tRoleOfflineTimers.getName()).getDatabaseName(), _tRoleOfflineTimers);
        zeze.addTable(zeze.getConfig().getTableConf(_tRoleTimers.getName()).getDatabaseName(), _tRoleTimers);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tAccountOfflineTimers.getName()).getDatabaseName(), _tAccountOfflineTimers);
        zeze.removeTable(zeze.getConfig().getTableConf(_tAccountTimers.getName()).getDatabaseName(), _tAccountTimers);
        zeze.removeTable(zeze.getConfig().getTableConf(_tCustomClasses.getName()).getDatabaseName(), _tCustomClasses);
        zeze.removeTable(zeze.getConfig().getTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
        zeze.removeTable(zeze.getConfig().getTableConf(_tRoleOfflineTimers.getName()).getDatabaseName(), _tRoleOfflineTimers);
        zeze.removeTable(zeze.getConfig().getTableConf(_tRoleTimers.getName()).getDatabaseName(), _tRoleTimers);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
