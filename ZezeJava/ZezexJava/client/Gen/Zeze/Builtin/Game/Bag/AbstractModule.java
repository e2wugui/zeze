// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 11014;
    public static final String ModuleName = "Bag";
    public static final String ModuleFullName = "Zeze.Builtin.Game.Bag";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ResultCodeFromInvalid = 1;
    public static final int ResultCodeToInvalid = 2;
    public static final int ResultCodeFromNotExist = 3;
    public static final int ResultCodeTrySplitButTargetExistDifferenceItem = 4;
    public static final int ResultCodeNumberInvalid = 6; // number==0 拒绝；-1表示全部仍为合法契约
    public static final int ResultCodeNotLogin = 7;
    public static final int ResultCodeBagNotExist = 8; // 内建协议非建行访问：目标包裹行不存在

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Bag.Move.class, Zeze.Builtin.Game.Bag.Move.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Bag.Move::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessMoveResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessMoveResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47308274693689L, factoryHandle); // 11014, -790071751
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Bag.Destroy.class, Zeze.Builtin.Game.Bag.Destroy.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Bag.Destroy::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDestroyResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessDestroyResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47307869964755L, factoryHandle); // 11014, -1194800685
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(47308274693689L);
        App.ClientService.getFactorys().remove(47307869964755L);
        // unregister table
        // unregister servlet
    }
}
