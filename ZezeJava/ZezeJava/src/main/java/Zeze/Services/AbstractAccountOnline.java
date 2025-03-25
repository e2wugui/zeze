// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractAccountOnline implements Zeze.IModule {
    public static final int ModuleId = 11041;
    public static final String ModuleName = "AccountOnline";
    public static final String ModuleFullName = "Zeze.Services.AccountOnline";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.AccountOnline.Kick.class, Zeze.Builtin.AccountOnline.Kick.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.AccountOnline.Kick::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKickResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKickResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47424687640437L, factoryHandle); // 11041, -341241995
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.AccountOnline.Login.class, Zeze.Builtin.AccountOnline.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.AccountOnline.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47421726655436L, factoryHandle); // 11041, 992740300
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.AccountOnline.Logout.class, Zeze.Builtin.AccountOnline.Logout.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.AccountOnline.Logout::new;
            factoryHandle.Handle = this::ProcessLogoutRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLogoutRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLogoutRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47422456981759L, factoryHandle); // 11041, 1723066623
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.AccountOnline.Register.class, Zeze.Builtin.AccountOnline.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.AccountOnline.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47423049070847L, factoryHandle); // 11041, -1979811585
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47424687640437L);
        service.getFactorys().remove(47421726655436L);
        service.getFactorys().remove(47422456981759L);
        service.getFactorys().remove(47423049070847L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessLoginRequest(Zeze.Builtin.AccountOnline.Login r) throws Exception;
    protected abstract long ProcessLogoutRequest(Zeze.Builtin.AccountOnline.Logout r) throws Exception;
    protected abstract long ProcessRegisterRequest(Zeze.Builtin.AccountOnline.Register r) throws Exception;
}
