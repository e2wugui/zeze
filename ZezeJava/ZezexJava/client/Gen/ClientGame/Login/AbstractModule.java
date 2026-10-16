// auto-generated @formatter:off
package ClientGame.Login;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 1;
    public static final String ModuleName = "Login";
    public static final String ModuleFullName = "ClientGame.Login";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ResultCodeCreateRoleDuplicateRoleName = 1;

    public final ClientGame.App App;

    public AbstractModule(ClientGame.App app) {
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Login.CreateRole.class, ClientGame.Login.CreateRole.TypeId_);
            factoryHandle.Factory = ClientGame.Login.CreateRole::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateRoleResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateRoleResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(8319355361L, factoryHandle); // 1, -270579231
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Login.GetRoleList.class, ClientGame.Login.GetRoleList.TypeId_);
            factoryHandle.Factory = ClientGame.Login.GetRoleList::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetRoleListResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetRoleListResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(4551632983L, factoryHandle); // 1, 256665687
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(8319355361L);
        App.ClientService.getFactorys().remove(4551632983L);
        // unregister table
        // unregister servlet
    }
}
