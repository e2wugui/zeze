// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractDelayRemove extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Builtin.DelayRemove"; }
    @Override public String getName() { return "DelayRemove"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11007;

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
