// auto-generated @formatter:off
package Zeze.World;

public abstract class AbstractWorld implements Zeze.IModule {
    public static final int ModuleId = 11031;
    public static final String ModuleName = "World";
    public static final String ModuleFullName = "Zeze.World.World";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Move.class, Zeze.Builtin.World.Move.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Move::new;
            factoryHandle.Handle = this::ProcessMove;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessMove", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessMove", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47379751479219L, factoryHandle); // 11031, 1967237043
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Rotate.class, Zeze.Builtin.World.Rotate.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Rotate::new;
            factoryHandle.Handle = this::ProcessRotate;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRotate", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRotate", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47377798104423L, factoryHandle); // 11031, 13862247
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Stop.class, Zeze.Builtin.World.Stop.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Stop::new;
            factoryHandle.Handle = this::ProcessStop;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessStop", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessStop", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47380680357614L, factoryHandle); // 11031, -1398851858
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47379751479219L);
        service.getFactorys().remove(47377798104423L);
        service.getFactorys().remove(47380680357614L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessMove(Zeze.Builtin.World.Move p) throws Exception;
    protected abstract long ProcessRotate(Zeze.Builtin.World.Rotate p) throws Exception;
    protected abstract long ProcessStop(Zeze.Builtin.World.Stop p) throws Exception;
}
