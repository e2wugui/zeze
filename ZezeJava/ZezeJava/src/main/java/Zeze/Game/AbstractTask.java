// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractTask extends Zeze.IModule {
    public static final int ModuleId = 11018;
    @Override public String getFullName() { return "Zeze.Game.Task"; }
    @Override public String getName() { return "Task"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public static final int Disabled = 0; // 可接取
    public static final int Init = 0; // 可接取
    public static final int Processing = 1; // 未完成，已经接取
    public static final int Finish = 2; // 已完成，未提交
    public static final int Committed = 3; // 已经提交

    protected final Zeze.Builtin.Game.Task.tTask _tTask = new Zeze.Builtin.Game.Task.tTask();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tTask.getName()).getDatabaseName(), _tTask);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
