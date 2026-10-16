// auto-generated @formatter:off
package ClientGame.Fight;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 4;
    public static final String ModuleName = "Fight";
    public static final String ModuleFullName = "ClientGame.Fight";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract long ProcessAreYouFightRequest(ClientGame.Fight.AreYouFight r) throws Exception;

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientGame.Fight.AreYouFight.class, ClientGame.Fight.AreYouFight.TypeId_);
            factoryHandle.Factory = ClientGame.Fight.AreYouFight::new;
            factoryHandle.Handle = this::ProcessAreYouFightRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAreYouFightRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAreYouFightRequest", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(17679878184L, factoryHandle); // 4, 500009000
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(17679878184L);
        // unregister table
        // unregister servlet
    }
}
