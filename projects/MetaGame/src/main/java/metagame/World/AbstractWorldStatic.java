// auto-generated @formatter:off
package metagame.World;

public abstract class AbstractWorldStatic implements Zeze.IModule {
    public static final int ModuleId = 10003;
    public static final String ModuleName = "WorldStatic";
    public static final String ModuleFullName = "metagame.World.WorldStatic";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.World.Static.SwitchWorld.class, metagame.builtin.World.Static.SwitchWorld.TypeId_);
            factoryHandle.Factory = metagame.builtin.World.Static.SwitchWorld::new;
            factoryHandle.Handle = this::ProcessSwitchWorldRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSwitchWorldRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSwitchWorldRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42965063354644L, factoryHandle); // 10003, -1789474540
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(42965063354644L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessSwitchWorldRequest(metagame.builtin.World.Static.SwitchWorld r) throws Exception;
}
