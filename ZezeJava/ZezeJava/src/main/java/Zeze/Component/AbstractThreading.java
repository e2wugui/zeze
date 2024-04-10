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

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eEnterRead = 0;
    public static final int eEnterWrite = 1;
    public static final int eExitRead = 2;
    public static final int eExitWrite = 3;

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.ReadWriteLockOperate.class, Zeze.Builtin.Threading.ReadWriteLockOperate.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.ReadWriteLockOperate::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReadWriteLockOperateResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReadWriteLockOperateResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47376860983435L, factoryHandle); // 11030, -923258741
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.SemaphoreCreate.class, Zeze.Builtin.Threading.SemaphoreCreate.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.SemaphoreCreate::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSemaphoreCreateResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSemaphoreCreateResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47377757945276L, factoryHandle); // 11030, -26296900
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.SemaphoreRelease.class, Zeze.Builtin.Threading.SemaphoreRelease.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.SemaphoreRelease::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSemaphoreReleaseResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSemaphoreReleaseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47374374546810L, factoryHandle); // 11030, 885271930
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Threading.SemaphoreTryAcquire.class, Zeze.Builtin.Threading.SemaphoreTryAcquire.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Threading.SemaphoreTryAcquire::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSemaphoreTryAcquireResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSemaphoreTryAcquireResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47376407442804L, factoryHandle); // 11030, -1376799372
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47375642163702L);
        service.getFactorys().remove(47374259242978L);
        service.getFactorys().remove(47376860983435L);
        service.getFactorys().remove(47377757945276L);
        service.getFactorys().remove(47374374546810L);
        service.getFactorys().remove(47376407442804L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
