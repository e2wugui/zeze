// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractRedoQueueServer extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Beans.RedoQueue"; }
    @Override public String getName() { return "RedoQueue"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11010;

    protected final Zeze.Beans.RedoQueue.tQueueLastTaskId _tQueueLastTaskId = new Zeze.Beans.RedoQueue.tQueueLastTaskId();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.RedoQueue.RunTask>();
            factoryHandle.Factory = Zeze.Beans.RedoQueue.RunTask::new;
            factoryHandle.Handle = this::ProcessRunTaskRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRunTaskRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47289196145593L, factoryHandle); // 11010, 1606216633
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47289196145593L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tQueueLastTaskId.getName()).getDatabaseName(), _tQueueLastTaskId);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tQueueLastTaskId.getName()).getDatabaseName(), _tQueueLastTaskId);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessRunTaskRequest(Zeze.Beans.RedoQueue.RunTask r) throws Throwable;
}
