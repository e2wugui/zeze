// auto-generated @formatter:off
package Zeze.World;

public abstract class AbstractWorldStatic implements Zeze.IModule {
    public static final int ModuleId = 11032;
    public static final String ModuleName = "WorldStatic";
    public static final String ModuleFullName = "Zeze.World.WorldStatic";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Static.SwitchWorld.class, Zeze.Builtin.World.Static.SwitchWorld.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Static.SwitchWorld::new;
            factoryHandle.Handle = this::ProcessSwitchWorldRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSwitchWorldRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSwitchWorldRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47385820582911L, factoryHandle); // 11032, -553593857
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47385820582911L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessSwitchWorldRequest(Zeze.Builtin.World.Static.SwitchWorld r) throws Exception;
}
