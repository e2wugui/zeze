// auto-generated @formatter:off
package Zeze.Collections;

public abstract class AbstractBoolList implements Zeze.IModule {
    public static final int ModuleId = 11034;
    public static final String ModuleName = "BoolList";
    public static final String ModuleFullName = "Zeze.Collections.BoolList";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    protected final Zeze.Builtin.Collections.BoolList.tBoolList _tBoolList = new Zeze.Builtin.Collections.BoolList.tBoolList();

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tBoolList.getName()).getDatabaseName(), _tBoolList);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tBoolList.getName()).getDatabaseName(), _tBoolList);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
