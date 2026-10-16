// auto-generated @formatter:off
package Game.Login;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 1;
    public static final String ModuleName = "Login";
    public static final String ModuleFullName = "Game.Login";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ResultCodeCreateRoleDuplicateRoleName = 1;

    protected abstract long ProcessCreateRoleRequest(Game.Login.CreateRole r) throws Exception;
    protected abstract long ProcessGetRoleListRequest(Game.Login.GetRoleList r) throws Exception;

    protected final Game.Login.taccount _taccount;
    protected final Game.Login.trole _trole;
    protected final Game.Login.trolename _trolename;

    public final Game.App App;

    public AbstractModule(Game.App app) {
        _taccount = new Game.Login.taccount();
        _trole = new Game.Login.trole();
        _trolename = new Game.Login.trolename();
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Login.CreateRole.class, Game.Login.CreateRole.TypeId_);
            factoryHandle.Factory = Game.Login.CreateRole::new;
            factoryHandle.Handle = this::ProcessCreateRoleRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateRoleRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateRoleRequest", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(8319355361L, factoryHandle); // 1, -270579231
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Login.GetRoleList.class, Game.Login.GetRoleList.TypeId_);
            factoryHandle.Factory = Game.Login.GetRoleList::new;
            factoryHandle.Handle = this::ProcessGetRoleListRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetRoleListRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetRoleListRequest", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(4551632983L, factoryHandle); // 1, 256665687
        }
        // register table
        if (App.Zeze != null) {
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_taccount.getName())).getDatabaseName(), _taccount);
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_trole.getName())).getDatabaseName(), _trole);
            App.Zeze.replaceTable(java.util.Objects.requireNonNull(App.Zeze.getConfig().getTableConf(_trolename.getName())).getDatabaseName(), _trolename);
        }
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.Server.getFactorys().remove(8319355361L);
        App.Server.getFactorys().remove(4551632983L);
        // unregister table
        // unregister servlet
    }
}
