// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractLoginQueueClient implements Zeze.IModule {
    public static final int ModuleId = 11043;
    public static final String ModuleName = "LoginQueueClient";
    public static final String ModuleFullName = "Zeze.Services.LoginQueueClient";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LoginQueue.PutQueuePosition.class, Zeze.Builtin.LoginQueue.PutQueuePosition.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LoginQueue.PutQueuePosition::new;
            factoryHandle.Handle = this::ProcessPutQueuePosition;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPutQueuePosition", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPutQueuePosition", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47432615378605L, factoryHandle); // 11043, -1003438419
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LoginQueue.PutLoginToken.class, Zeze.Builtin.LoginQueue.PutLoginToken.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LoginQueue.PutLoginToken::new;
            factoryHandle.Handle = this::ProcessPutLoginToken;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPutLoginToken", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPutLoginToken", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47431216900463L, factoryHandle); // 11043, 1893050735
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LoginQueue.PutQueueFull.class, Zeze.Builtin.LoginQueue.PutQueueFull.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LoginQueue.PutQueueFull::new;
            factoryHandle.Handle = this::ProcessPutQueueFull;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPutQueueFull", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPutQueueFull", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47432287199628L, factoryHandle); // 11043, -1331617396
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47432615378605L);
        service.getFactorys().remove(47431216900463L);
        service.getFactorys().remove(47432287199628L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessPutQueuePosition(Zeze.Builtin.LoginQueue.PutQueuePosition p) throws Exception;
    protected abstract long ProcessPutLoginToken(Zeze.Builtin.LoginQueue.PutLoginToken p) throws Exception;
    protected abstract long ProcessPutQueueFull(Zeze.Builtin.LoginQueue.PutQueueFull p) throws Exception;
}
