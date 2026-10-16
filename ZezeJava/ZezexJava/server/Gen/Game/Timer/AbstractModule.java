// auto-generated @formatter:off
package Game.Timer;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 11;
    public static final String ModuleName = "Timer";
    public static final String ModuleFullName = "Game.Timer";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Game.Timer.tIndexs _tIndexs;
    protected final Game.Timer.tNodeRoot _tNodeRoot;
    protected final Game.Timer.tNodes _tNodes;

    public final Game.App App;

    public AbstractModule(Game.App app) {
        _tIndexs = new Game.Timer.tIndexs();
        _tNodeRoot = new Game.Timer.tNodeRoot();
        _tNodes = new Game.Timer.tNodes();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        // register table
        if (App.Zeze != null) {
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tIndexs.getName())).getDatabaseName(), _tIndexs);
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tNodeRoot.getName())).getDatabaseName(), _tNodeRoot);
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tNodes.getName())).getDatabaseName(), _tNodes);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        // unregister table
        // unregister servlet
    }
}
