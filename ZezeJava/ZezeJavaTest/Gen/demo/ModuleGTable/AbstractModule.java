// auto-generated @formatter:off
package demo.ModuleGTable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 100;
    public static final String ModuleName = "ModuleGTable";
    public static final String ModuleFullName = "demo.ModuleGTable";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final demo.ModuleGTable.tGTable _tGTable;
    protected final demo.ModuleGTable.tGTable2 _tGTable2;

    public final demo.App App;

    public AbstractModule(demo.App app) {
        _tGTable = new demo.ModuleGTable.tGTable();
        _tGTable2 = new demo.ModuleGTable.tGTable2();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        // register table
        if (App.Zeze != null) {
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tGTable.getName())).getDatabaseName(), _tGTable);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tGTable2.getName())).getDatabaseName(), _tGTable2);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        // unregister table
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tGTable.getName())).getDatabaseName(), _tGTable);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tGTable2.getName())).getDatabaseName(), _tGTable2);
        // unregister servlet
    }
}
