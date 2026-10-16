// auto-generated @formatter:off
package demo.web;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 4;
    public static final String ModuleName = "Web";
    public static final String ModuleFullName = "demo.web";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract void OnServlethellocount(Zeze.Netty.HttpExchange x) throws Exception;

    protected final demo.web.tHelloCount _tHelloCount;
    protected final demo.web.tMap2Bean1 _tMap2Bean1;

    public final demo.App App;

    public AbstractModule(demo.App app) {
        _tHelloCount = new demo.web.tHelloCount("__1");
        _tMap2Bean1 = new demo.web.tMap2Bean1();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        // register table
        if (App.Zeze != null) {
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tHelloCount.getName())).getDatabaseName(), _tHelloCount);
            App.Zeze.addTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tMap2Bean1.getName())).getDatabaseName(), _tMap2Bean1);
        }
        // register servlet
        var _reflect = new Zeze.Util.Reflect(getClass());
        App.getHttpServer().addHandler("/demo/web/hellocount", 8192,
                _reflect.getTransactionLevel("OnServlethellocount", Zeze.Transaction.TransactionLevel.Serializable),
                _reflect.getDispatchMode("OnServlethellocount", Zeze.Transaction.DispatchMode.Normal),
                this::OnServlethellocount);
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        // unregister table
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tHelloCount.getName())).getDatabaseName(), _tHelloCount);
        App.Zeze.removeTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tMap2Bean1.getName())).getDatabaseName(), _tMap2Bean1);
        // unregister servlet
        App.getHttpServer().removeHandler("/demo/web/hellocount");
    }
}
