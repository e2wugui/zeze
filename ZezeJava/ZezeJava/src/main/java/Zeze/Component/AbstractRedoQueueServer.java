// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractRedoQueueServer implements Zeze.IModule {
    public static final int ModuleId = 11010;
    public static final String ModuleName = "RedoQueueServer";
    public static final String ModuleFullName = "Zeze.Component.RedoQueueServer";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    protected final Zeze.Builtin.RedoQueue.tQueueLastTaskId _tQueueLastTaskId = new Zeze.Builtin.RedoQueue.tQueueLastTaskId();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.RedoQueue.RunTask.class, Zeze.Builtin.RedoQueue.RunTask.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.RedoQueue.RunTask::new;
            factoryHandle.Handle = this::ProcessRunTaskRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRunTaskRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRunTaskRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47289120801215L, factoryHandle); // 11010, 1530872255
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47289120801215L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tQueueLastTaskId.getName()).getDatabaseName(), _tQueueLastTaskId);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tQueueLastTaskId.getName()).getDatabaseName(), _tQueueLastTaskId);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessRunTaskRequest(Zeze.Builtin.RedoQueue.RunTask r) throws Exception;
}
