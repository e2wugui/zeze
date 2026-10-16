// auto-generated @formatter:off
package Zeze.Builtin.LinkdBase;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 11011;
    public static final String ModuleName = "LinkdBase";
    public static final String ModuleFullName = "Zeze.Builtin.LinkdBase";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract long ProcessReportError(Zeze.Builtin.LinkdBase.ReportError p) throws Exception;

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LinkdBase.ReportError.class, Zeze.Builtin.LinkdBase.ReportError.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LinkdBase.ReportError::new;
            factoryHandle.Handle = this::ProcessReportError;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportError", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportError", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47292803771063L, factoryHandle); // 11011, 918874807
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(47292803771063L);
        // unregister table
        // unregister servlet
    }
}
