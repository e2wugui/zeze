// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractAuth implements Zeze.IModule {
    public static final int ModuleId = 11036;
    public static final String ModuleName = "Auth";
    public static final String ModuleFullName = "Zeze.Component.Auth";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Auth.tAuth _tAuth = new Zeze.Builtin.Auth.tAuth();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tAuth.getName()).getDatabaseName(), _tAuth);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tAuth.getName()).getDatabaseName(), _tAuth);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
