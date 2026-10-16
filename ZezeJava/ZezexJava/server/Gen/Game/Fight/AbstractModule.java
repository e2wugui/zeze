// auto-generated @formatter:off
package Game.Fight;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 4;
    public static final String ModuleName = "Fight";
    public static final String ModuleFullName = "Game.Fight";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Game.Fight.tfighters _tfighters;

    public final Game.App App;

    public AbstractModule(Game.App app) {
        _tfighters = new Game.Fight.tfighters();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Fight.AreYouFight.class, Game.Fight.AreYouFight.TypeId_);
            factoryHandle.Factory = Game.Fight.AreYouFight::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAreYouFightResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAreYouFightResponse", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(17679878184L, factoryHandle); // 4, 500009000
        }
        // register table
        if (App.Zeze != null) {
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_tfighters.getName())).getDatabaseName(), _tfighters);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.Server.getFactorys().remove(17679878184L);
        // unregister table
        // unregister servlet
    }
}
