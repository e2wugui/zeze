// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractAutoKey extends Zeze.IModule {
    public static final int ModuleId = 11003;
    @Override public String getFullName() { return "Zeze.Component.AutoKey"; }
    @Override public String getName() { return "AutoKey"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.AutoKey.tAutoKeys _tAutoKeys = new Zeze.Builtin.AutoKey.tAutoKeys();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tAutoKeys.getName()).getDatabaseName(), _tAutoKeys);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tAutoKeys.getName()).getDatabaseName(), _tAutoKeys);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }

}
