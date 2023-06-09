// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractThreading implements Zeze.IModule {
    public static final int ModuleId = 11030;
    public static final String ModuleName = "Threading";
    public static final String ModuleFullName = "Zeze.Component.Threading";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.MutexTryLock.class, Zeze.Builtin.Threading.MutexTryLock.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.MutexTryLock::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessMutexTryLockResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessMutexTryLockResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47375642163702L, factoryHandle); // 11030, -2142078474
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.MutexUnlock.class, Zeze.Builtin.Threading.MutexUnlock.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.MutexUnlock::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessMutexUnlockResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessMutexUnlockResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47374259242978L, factoryHandle); // 11030, 769968098
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.QueryLockInfo.class, Zeze.Builtin.Threading.QueryLockInfo.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.QueryLockInfo::new;
            factoryHandle.Handle = this::ProcessQueryLockInfoRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryLockInfoRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryLockInfoRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47373607783577L, factoryHandle); // 11030, 118508697
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47375642163702L);
        service.getFactorys().remove(47374259242978L);
        service.getFactorys().remove(47373607783577L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessQueryLockInfoRequest(Zeze.Builtin.Threading.QueryLockInfo r) throws Exception;
}
