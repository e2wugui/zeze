// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractDelayRemove extends Zeze.IModule {
    public static final int ModuleId = 11007;
    @Override public String getFullName() { return "Zeze.Component.DelayRemove"; }
    @Override public String getName() { return "DelayRemove"; }
    @Override public int getId() { return ModuleId; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
