// auto-generated @formatter:off
package Game.Buf;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 6;
    public static final String ModuleName = "Buf";
    public static final String ModuleFullName = "Game.Buf";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Game.Buf.tbufs _tbufs;

    public final Game.App App;

    public AbstractModule(Game.App app) {
        _tbufs = new Game.Buf.tbufs();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        // register table
        if (App.Zeze != null) {
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tbufs.getName())).getDatabaseName(), _tbufs);
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
