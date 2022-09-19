// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractTimer extends Zeze.IModule {
    public static final int ModuleId = 11016;
    @Override public String getFullName() { return "Zeze.Component.Timer"; }
    @Override public String getName() { return "Timer"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Timer.tArchOlineTimer _tArchOlineTimer = new Zeze.Builtin.Timer.tArchOlineTimer();
    protected final Zeze.Builtin.Timer.tCustomClasses _tCustomClasses = new Zeze.Builtin.Timer.tCustomClasses();
    protected final Zeze.Builtin.Timer.tGameOlineTimer _tGameOlineTimer = new Zeze.Builtin.Timer.tGameOlineTimer();
    protected final Zeze.Builtin.Timer.tIndexs _tIndexs = new Zeze.Builtin.Timer.tIndexs();
    protected final Zeze.Builtin.Timer.tNamed _tNamed = new Zeze.Builtin.Timer.tNamed();
    protected final Zeze.Builtin.Timer.tNodeRoot _tNodeRoot = new Zeze.Builtin.Timer.tNodeRoot();
    protected final Zeze.Builtin.Timer.tNodes _tNodes = new Zeze.Builtin.Timer.tNodes();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tArchOlineTimer.getName()).getDatabaseName(), _tArchOlineTimer);
        zeze.addTable(zeze.getConfig().getTableConf(_tCustomClasses.getName()).getDatabaseName(), _tCustomClasses);
        zeze.addTable(zeze.getConfig().getTableConf(_tGameOlineTimer.getName()).getDatabaseName(), _tGameOlineTimer);
        zeze.addTable(zeze.getConfig().getTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.addTable(zeze.getConfig().getTableConf(_tNamed.getName()).getDatabaseName(), _tNamed);
        zeze.addTable(zeze.getConfig().getTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.addTable(zeze.getConfig().getTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tArchOlineTimer.getName()).getDatabaseName(), _tArchOlineTimer);
        zeze.removeTable(zeze.getConfig().getTableConf(_tCustomClasses.getName()).getDatabaseName(), _tCustomClasses);
        zeze.removeTable(zeze.getConfig().getTableConf(_tGameOlineTimer.getName()).getDatabaseName(), _tGameOlineTimer);
        zeze.removeTable(zeze.getConfig().getTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNamed.getName()).getDatabaseName(), _tNamed);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.removeTable(zeze.getConfig().getTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
