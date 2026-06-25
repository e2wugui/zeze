// auto-generated @formatter:off
package metagame.Task;

public abstract class AbstractTaskModule implements Zeze.IModule {
    public static final int ModuleId = 10004;
    public static final String ModuleName = "TaskModule";
    public static final String ModuleFullName = "metagame.Task.TaskModule";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eTaskCondition = 1;
    public static final int eTaskNotExists = 2;
    public static final int eTaskAlreadyAccepted = 3;
    public static final int eTaskTooManyAccepted = 4;
    public static final int eTaskNotDone = 5;
    public static final int eRewardNotExists = 6;
    public static final int eTaskNotAccepted = 7;
    public static final int eFinishError = 8;
    public static final int eTaskAccepted = 0; // 任务接受时的初始状态
    public static final int eTaskDone = 1; // 任务完成状态（未发放奖励）

    protected final metagame.builtin.TaskModule.tRoleTasks _tRoleTasks = new metagame.builtin.TaskModule.tRoleTasks();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.TaskModule.Accept.class, metagame.builtin.TaskModule.Accept.TypeId_);
            factoryHandle.Factory = metagame.builtin.TaskModule.Accept::new;
            factoryHandle.Handle = this::ProcessAcceptRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAcceptRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAcceptRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42970845973392L, factoryHandle); // 10004, -301823088
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.TaskModule.Finish.class, metagame.builtin.TaskModule.Finish.TypeId_);
            factoryHandle.Factory = metagame.builtin.TaskModule.Finish::new;
            factoryHandle.Handle = this::ProcessFinishRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFinishRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFinishRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42969974109650L, factoryHandle); // 10004, -1173686830
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.TaskModule.Abandon.class, metagame.builtin.TaskModule.Abandon.TypeId_);
            factoryHandle.Factory = metagame.builtin.TaskModule.Abandon::new;
            factoryHandle.Handle = this::ProcessAbandonRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAbandonRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAbandonRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42970538345884L, factoryHandle); // 10004, -609450596
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.TaskModule.GetRoleTasks.class, metagame.builtin.TaskModule.GetRoleTasks.TypeId_);
            factoryHandle.Factory = metagame.builtin.TaskModule.GetRoleTasks::new;
            factoryHandle.Handle = this::ProcessGetRoleTasksRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetRoleTasksRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetRoleTasksRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42969759006172L, factoryHandle); // 10004, -1388790308
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(42970845973392L);
        service.getFactorys().remove(42969974109650L);
        service.getFactorys().remove(42970538345884L);
        service.getFactorys().remove(42969759006172L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tRoleTasks.getName()).getDatabaseName(), _tRoleTasks);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tRoleTasks.getName()).getDatabaseName(), _tRoleTasks);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAcceptRequest(metagame.builtin.TaskModule.Accept r) throws Exception;
    protected abstract long ProcessFinishRequest(metagame.builtin.TaskModule.Finish r) throws Exception;
    protected abstract long ProcessAbandonRequest(metagame.builtin.TaskModule.Abandon r) throws Exception;
    protected abstract long ProcessGetRoleTasksRequest(metagame.builtin.TaskModule.GetRoleTasks r) throws Exception;
}
