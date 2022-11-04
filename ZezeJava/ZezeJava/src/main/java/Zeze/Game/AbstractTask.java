// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractTask extends Zeze.IModule {
    public static final int ModuleId = 11018;
    @Override public String getFullName() { return "Zeze.Game.Task"; }
    @Override public String getName() { return "Task"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Game.Task.tTask _tTask = new Zeze.Builtin.Game.Task.tTask();
    protected final Zeze.Builtin.Game.Task.tTaskCondition _tTaskCondition = new Zeze.Builtin.Game.Task.tTaskCondition();
    protected final Zeze.Builtin.Game.Task.tTaskPhase _tTaskPhase = new Zeze.Builtin.Game.Task.tTaskPhase();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Game.Task.CompleteCondition>();
            factoryHandle.Factory = Zeze.Builtin.Game.Task.CompleteCondition::new;
            factoryHandle.Handle = this::ProcessCompleteConditionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCompleteConditionRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCompleteConditionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47323389574082L, factoryHandle); // 11018, 1439906754
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47323389574082L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
        zeze.addTable(zeze.getConfig().getTableConf(_tTaskCondition.getName()).getDatabaseName(), _tTaskCondition);
        zeze.addTable(zeze.getConfig().getTableConf(_tTaskPhase.getName()).getDatabaseName(), _tTaskPhase);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
        zeze.removeTable(zeze.getConfig().getTableConf(_tTaskCondition.getName()).getDatabaseName(), _tTaskCondition);
        zeze.removeTable(zeze.getConfig().getTableConf(_tTaskPhase.getName()).getDatabaseName(), _tTaskPhase);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCompleteConditionRequest(Zeze.Builtin.Game.Task.CompleteCondition r) throws Throwable;
}
