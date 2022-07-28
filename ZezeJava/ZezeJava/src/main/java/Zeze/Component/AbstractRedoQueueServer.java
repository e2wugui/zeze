// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractRedoQueueServer extends Zeze.IModule {
    public static final int ModuleId = 11010;
    @Override public String getFullName() { return "Zeze.Component.RedoQueueServer"; }
    @Override public String getName() { return "RedoQueueServer"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.RedoQueue.tQueueLastTaskId _tQueueLastTaskId = new Zeze.Builtin.RedoQueue.tQueueLastTaskId();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.RedoQueue.RunTask>();
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
        zeze.AddTable(zeze.getConfig().GetTableConf(_tQueueLastTaskId.getName()).getDatabaseName(), _tQueueLastTaskId);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tQueueLastTaskId.getName()).getDatabaseName(), _tQueueLastTaskId);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessRunTaskRequest(Zeze.Builtin.RedoQueue.RunTask r) throws Throwable;
}
