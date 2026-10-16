// auto-generated @formatter:off
package demo.Game;

/*
L 场景（LMmoGameSim）MMO-lite 游戏模拟的持久世界 schema：大厅域事务落库；
	     房间域战斗状态机在内存（GameSim 包内纯 Java），不经任何表。
*/
@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 5;
    public static final String ModuleName = "Game";
    public static final String ModuleFullName = "demo.Game";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final demo.Game.tMail _tMail;
    protected final demo.Game.tRole _tRole;
    protected final demo.Game.tSession _tSession;

    public final demo.App App;

    public AbstractModule(demo.App app) {
        _tMail = new demo.Game.tMail();
        _tRole = new demo.Game.tRole();
        _tSession = new demo.Game.tSession();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        // register table
        if (App.Zeze != null) {
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tMail.getName())).getDatabaseName(), _tMail);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tRole.getName())).getDatabaseName(), _tRole);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tSession.getName())).getDatabaseName(), _tSession);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        // unregister table
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tMail.getName())).getDatabaseName(), _tMail);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tRole.getName())).getDatabaseName(), _tRole);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tSession.getName())).getDatabaseName(), _tSession);
        // unregister servlet
    }
}
