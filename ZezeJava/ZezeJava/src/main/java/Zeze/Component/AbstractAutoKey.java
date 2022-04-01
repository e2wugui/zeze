// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractAutoKey extends Zeze.IModule {
    public String getFullName() { return "Zeze.Beans.AutoKey"; }
    public String getName() { return "AutoKey"; }
    public int getId() { return ModuleId; }
    public static final int ModuleId = 11003;

    protected final Zeze.Beans.AutoKey.tAutoKeys _tAutoKeys = new Zeze.Beans.AutoKey.tAutoKeys();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tAutoKeys.getName()).getDatabaseName(), _tAutoKeys);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tAutoKeys.getName()).getDatabaseName(), _tAutoKeys);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
