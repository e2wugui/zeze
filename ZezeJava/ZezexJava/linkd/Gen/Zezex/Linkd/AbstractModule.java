// auto-generated @formatter:off
package Zezex.Linkd;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 10000;
    public static final String ModuleName = "Linkd";
    public static final String ModuleFullName = "Zezex.Linkd";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract long ProcessAuthRequest(Zezex.Linkd.Auth r) throws Exception;
    protected abstract long ProcessCs(Zezex.Linkd.Cs p) throws Exception;

    public final Zezex.App App;

    public AbstractModule(Zezex.App app) {
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zezex.Linkd.Auth.class, Zezex.Linkd.Auth.TypeId_);
            factoryHandle.Factory = Zezex.Linkd.Auth::new;
            factoryHandle.Handle = this::ProcessAuthRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAuthRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAuthRequest", Zeze.Transaction.DispatchMode.Normal);
            App.LinkdService.AddFactoryHandle(42952970027574L, factoryHandle); // 10000, -997899722
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zezex.Linkd.Cs.class, Zezex.Linkd.Cs.TypeId_);
            factoryHandle.Factory = Zezex.Linkd.Cs::new;
            factoryHandle.Handle = this::ProcessCs;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCs", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCs", Zeze.Transaction.DispatchMode.Normal);
            App.LinkdService.AddFactoryHandle(42950633967421L, factoryHandle); // 10000, 961007421
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.LinkdService.getFactorys().remove(42952970027574L);
        App.LinkdService.getFactorys().remove(42950633967421L);
        // unregister table
        // unregister servlet
    }
}
