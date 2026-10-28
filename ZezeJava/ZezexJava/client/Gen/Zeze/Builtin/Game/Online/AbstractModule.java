// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantSuppression", "RedundantThrows", "SameReturnValue"})
public abstract class AbstractModule implements Zeze.IModule {
    public static final int ModuleId = 11013;
    public static final String ModuleName = "Online";
    public static final String ModuleFullName = "Zeze.Builtin.Game.Online";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeCreateRoleDuplicateRoleName = 1;
    public static final int ResultCodeAccountNotExist = 2;
    public static final int ResultCodeRoleNotExist = 3;
    public static final int ResultCodeNotLastLoginRoleId = 4;
    public static final int ResultCodeOnlineDataNotFound = 5;
    public static final int ResultCodeReliableNotifyConfirmIndexOutOfRange = 6;
    public static final int ResultCodeNotLogin = 7;
    public static final int ResultCodeLoginSuperseded = 8; // 登录过程中已被其他成功登录取代
    public static final int eOffline = 0; // 离线状态
    public static final int eLinkBroken = 1; // 刚刚断开连接
    public static final int eLogined = 2; // 在线状态

    protected abstract long ProcessSReliableNotify(Zeze.Builtin.Game.Online.SReliableNotify p) throws Exception;

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.Login.class, Zeze.Builtin.Game.Online.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.Login::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47303980222879L, factoryHandle); // 11013, -789575265
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.ReLogin.class, Zeze.Builtin.Game.Online.ReLogin.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.ReLogin::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReLoginResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47304551116333L, factoryHandle); // 11013, -218681811
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.Logout.class, Zeze.Builtin.Game.Online.Logout.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.Logout::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLogoutResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLogoutResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47304205955457L, factoryHandle); // 11013, -563842687
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.ReliableNotifyConfirm.class, Zeze.Builtin.Game.Online.ReliableNotifyConfirm.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.ReliableNotifyConfirm::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReliableNotifyConfirmResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReliableNotifyConfirmResponse", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47304349755660L, factoryHandle); // 11013, -420042484
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Online.SReliableNotify.class, Zeze.Builtin.Game.Online.SReliableNotify.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Online.SReliableNotify::new;
            factoryHandle.Handle = this::ProcessSReliableNotify;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSReliableNotify", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSReliableNotify", Zeze.Transaction.DispatchMode.Normal);
            App.ClientService.AddFactoryHandle(47302416296295L, factoryHandle); // 11013, 1941465447
        }
        // register table
        // register servlet
    }

    @Override
    public void UnRegister() {
        // unregister protocol factory and handles
        App.ClientService.getFactorys().remove(47303980222879L);
        App.ClientService.getFactorys().remove(47304551116333L);
        App.ClientService.getFactorys().remove(47304205955457L);
        App.ClientService.getFactorys().remove(47304349755660L);
        App.ClientService.getFactorys().remove(47302416296295L);
        // unregister table
        // unregister servlet
    }
}
