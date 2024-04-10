// auto-generated @formatter:off
package Zeze.Onz;

public abstract class AbstractOnz implements Zeze.IModule {
    public static final int ModuleId = 11038;
    public static final String ModuleName = "Onz";
    public static final String ModuleFullName = "Zeze.Onz.Onz";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eProcedureNotFound = 1;
    public static final int eSagaNotFound = 2;
    public static final int eSagaTidExist = 3;
    public static final int eOnzTidNotFound = 4;
    public static final int eRollback = 5;
    public static final int eFlushAsync = 1;
    public static final int eFlushImmediately = 2;
    public static final int eCommitNotExist = 0;
    public static final int ePreparing = 1;
    public static final int eCommitting = 2;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Checkpoint.class, Zeze.Builtin.Onz.Checkpoint.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.Checkpoint::new;
            factoryHandle.Handle = this::ProcessCheckpointRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCheckpointRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCheckpointRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47411858488468L, factoryHandle); // 11038, -285492076
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Commit.class, Zeze.Builtin.Onz.Commit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.Commit::new;
            factoryHandle.Handle = this::ProcessCommitRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47411106178581L, factoryHandle); // 11038, -1037801963
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FlushReady.class, Zeze.Builtin.Onz.FlushReady.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FlushReady::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFlushReadyResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFlushReadyResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410000793930L, factoryHandle); // 11038, -2143186614
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncProcedure.class, Zeze.Builtin.Onz.FuncProcedure.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncProcedure::new;
            factoryHandle.Handle = this::ProcessFuncProcedureRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncProcedureRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncProcedureRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410672249436L, factoryHandle); // 11038, -1471731108
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSaga.class, Zeze.Builtin.Onz.FuncSaga.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSaga::new;
            factoryHandle.Handle = this::ProcessFuncSagaRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47411539774123L, factoryHandle); // 11038, -604206421
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSagaEnd.class, Zeze.Builtin.Onz.FuncSagaEnd.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSagaEnd::new;
            factoryHandle.Handle = this::ProcessFuncSagaEndRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaEndRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaEndRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47409308020494L, factoryHandle); // 11038, 1459007246
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Rollback.class, Zeze.Builtin.Onz.Rollback.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.Rollback::new;
            factoryHandle.Handle = this::ProcessRollbackRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRollbackRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRollbackRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410112848658L, factoryHandle); // 11038, -2031131886
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47411858488468L);
        service.getFactorys().remove(47411106178581L);
        service.getFactorys().remove(47410000793930L);
        service.getFactorys().remove(47410672249436L);
        service.getFactorys().remove(47411539774123L);
        service.getFactorys().remove(47409308020494L);
        service.getFactorys().remove(47410112848658L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCheckpointRequest(Zeze.Builtin.Onz.Checkpoint r) throws Exception;
    protected abstract long ProcessCommitRequest(Zeze.Builtin.Onz.Commit r) throws Exception;
    protected abstract long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) throws Exception;
    protected abstract long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception;
    protected abstract long ProcessFuncSagaEndRequest(Zeze.Builtin.Onz.FuncSagaEnd r) throws Exception;
    protected abstract long ProcessRollbackRequest(Zeze.Builtin.Onz.Rollback r) throws Exception;
}
