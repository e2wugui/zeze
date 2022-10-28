// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractTask extends Zeze.IModule {
    public static final int ModuleId = 11018;
    @Override public String getFullName() { return "Zeze.Game.Task"; }
    @Override public String getName() { return "Task"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

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
