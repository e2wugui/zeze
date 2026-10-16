// auto-generated @formatter:off
package Game.Map;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 8;
    public static final String ModuleName = "Map";
    public static final String ModuleFullName = "Game.Map";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected abstract long ProcessCEnterWorld(Game.Map.CEnterWorld p) throws Exception;
    protected abstract long ProcessCEnterWorldDone(Game.Map.CEnterWorldDone p) throws Exception;

    public final Game.App App;

    public AbstractModule(Game.App app) {
        App = app;
        if (app != null)
            Register();
    }

    @Override
    public void Register() {
        // register protocol factory and handles
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Map.CEnterWorld.class, Game.Map.CEnterWorld.TypeId_);
            factoryHandle.Factory = Game.Map.CEnterWorld::new;
            factoryHandle.Handle = this::ProcessCEnterWorld;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCEnterWorld", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCEnterWorld", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(35514358966L, factoryHandle); // 8, 1154620598
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Game.Map.CEnterWorldDone.class, Game.Map.CEnterWorldDone.TypeId_);
            factoryHandle.Factory = Game.Map.CEnterWorldDone::new;
            factoryHandle.Handle = this::ProcessCEnterWorldDone;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCEnterWorldDone", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCEnterWorldDone", Zeze.Transaction.DispatchMode.Normal);
            App.Server.AddFactoryHandle(35705348604L, factoryHandle); // 8, 1345610236
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.Server.getFactorys().remove(35514358966L);
        App.Server.getFactorys().remove(35705348604L);
        // unregister table
        // unregister servlet
    }
}
