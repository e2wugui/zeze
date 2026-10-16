// auto-generated @formatter:off
package ClientZezex.Linkd;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 10000;
    public static final String ModuleName = "Linkd";
    public static final String ModuleFullName = "ClientZezex.Linkd";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract long ProcessSc(ClientZezex.Linkd.Sc p) throws Exception;

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientZezex.Linkd.Auth.class, ClientZezex.Linkd.Auth.TypeId_);
            factoryHandle.Factory = ClientZezex.Linkd.Auth::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAuthResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(42952970027574L, factoryHandle); // 10000, -997899722
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(ClientZezex.Linkd.Sc.class, ClientZezex.Linkd.Sc.TypeId_);
            factoryHandle.Factory = ClientZezex.Linkd.Sc::new;
            factoryHandle.Handle = this::ProcessSc;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSc", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSc", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(42952961972367L, factoryHandle); // 10000, -1005954929
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(42952970027574L);
        App.ClientService.getFactorys().remove(42952961972367L);
        // unregister table
        // unregister servlet
    }
}
