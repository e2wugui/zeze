// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractTask extends Zeze.IModule {
    public static final int ModuleId = 11018;
    @Override public String getFullName() { return "Zeze.Game.Task"; }
    @Override public String getName() { return "Task"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int Invalid = -1; // 可接取
    public static final int Disabled = 0; // 可接取
    public static final int Init = 1; // 可接取
    public static final int Processing = 2; // 未完成，已经接取
    public static final int Finish = 3; // 已完成，未提交
    public static final int Committed = 4; // 已经提交

    protected final Zeze.Builtin.Game.Task.tEventClasses _tEventClasses = new Zeze.Builtin.Game.Task.tEventClasses();
    protected final Zeze.Builtin.Game.Task.tRoleTask _tRoleTask = new Zeze.Builtin.Game.Task.tRoleTask();
    protected final Zeze.Builtin.Game.Task.tTask _tTask = new Zeze.Builtin.Game.Task.tTask();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.Game.Task.TriggerTaskEvent>();
            factoryHandle.Factory = Zeze.Builtin.Game.Task.TriggerTaskEvent::new;
            factoryHandle.Handle = this::ProcessTriggerTaskEventRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTriggerTaskEventRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTriggerTaskEventRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47324894444505L, factoryHandle); // 11018, -1350190119
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47324894444505L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tEventClasses.getName()).getDatabaseName(), _tEventClasses);
        zeze.addTable(zeze.getConfig().getTableConf(_tRoleTask.getName()).getDatabaseName(), _tRoleTask);
        zeze.addTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tEventClasses.getName()).getDatabaseName(), _tEventClasses);
        zeze.removeTable(zeze.getConfig().getTableConf(_tRoleTask.getName()).getDatabaseName(), _tRoleTask);
        zeze.removeTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }


    protected abstract long ProcessTriggerTaskEventRequest(Zeze.Builtin.Game.Task.TriggerTaskEvent r) throws Throwable;
}
