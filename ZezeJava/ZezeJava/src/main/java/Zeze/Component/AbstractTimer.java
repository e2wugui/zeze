// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractTimer extends Zeze.IModule {
    public static final int ModuleId = 11016;
    @Override public String getFullName() { return "Zeze.Component.Timer"; }
    @Override public String getName() { return "Timer"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Timer.tIndexs _tIndexs = new Zeze.Builtin.Timer.tIndexs();
    protected final Zeze.Builtin.Timer.tNodeRoot _tNodeRoot = new Zeze.Builtin.Timer.tNodeRoot();
    protected final Zeze.Builtin.Timer.tNodes _tNodes = new Zeze.Builtin.Timer.tNodes();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tIndexs.getName()).getDatabaseName(), _tIndexs);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tNodeRoot.getName()).getDatabaseName(), _tNodeRoot);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tNodes.getName()).getDatabaseName(), _tNodes);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
