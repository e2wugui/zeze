// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractTaskModule implements Zeze.IModule {
    public static final int ModuleId = 11018;
    public static final String ModuleName = "TaskModule";
    public static final String ModuleFullName = "Zeze.Game.TaskModule";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public static final int eTaskCondition = 1;
    public static final int eTaskNotExists = 2;
    public static final int eTaskAlreadyAccepted = 3;
    public static final int eTaskTooManyAccepted = 4;
    public static final int eTaskNotDone = 5;
    public static final int eRewardNotExists = 6;
    public static final int eTaskNotAccepted = 7;
    public static final int eTaskAccepted = 0; // 任务接受时的初始状态
    public static final int eTaskDone = 1; // 任务完成状态
    public static final int eTaskCompleted = 2; // 任务结束状态（已经发换奖励）

    protected final Zeze.Builtin.Game.TaskModule.tRoleTasks _tRoleTasks = new Zeze.Builtin.Game.TaskModule.tRoleTasks();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.TaskModule.Abandon.class, Zeze.Builtin.Game.TaskModule.Abandon.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.TaskModule.Abandon::new;
            factoryHandle.Handle = this::ProcessAbandonRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAbandonRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAbandonRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47322741362506L, factoryHandle); // 11018, 791695178
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.TaskModule.Accept.class, Zeze.Builtin.Game.TaskModule.Accept.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.TaskModule.Accept::new;
            factoryHandle.Handle = this::ProcessAcceptRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcceptRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAcceptRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47322259291035L, factoryHandle); // 11018, 309623707
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.TaskModule.Finish.class, Zeze.Builtin.Game.TaskModule.Finish.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.TaskModule.Finish::new;
            factoryHandle.Handle = this::ProcessFinishRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFinishRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFinishRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47324807161935L, factoryHandle); // 11018, -1437472689
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.TaskModule.GetRoleTasks.class, Zeze.Builtin.Game.TaskModule.GetRoleTasks.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.TaskModule.GetRoleTasks::new;
            factoryHandle.Handle = this::ProcessGetRoleTasksRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetRoleTasksRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetRoleTasksRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47326109130780L, factoryHandle); // 11018, -135503844
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47322741362506L);
        service.getFactorys().remove(47322259291035L);
        service.getFactorys().remove(47324807161935L);
        service.getFactorys().remove(47326109130780L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tRoleTasks.getName()).getDatabaseName(), _tRoleTasks);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tRoleTasks.getName()).getDatabaseName(), _tRoleTasks);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAbandonRequest(Zeze.Builtin.Game.TaskModule.Abandon r) throws Exception;
    protected abstract long ProcessAcceptRequest(Zeze.Builtin.Game.TaskModule.Accept r) throws Exception;
    protected abstract long ProcessFinishRequest(Zeze.Builtin.Game.TaskModule.Finish r) throws Exception;
    protected abstract long ProcessGetRoleTasksRequest(Zeze.Builtin.Game.TaskModule.GetRoleTasks r) throws Exception;
}
