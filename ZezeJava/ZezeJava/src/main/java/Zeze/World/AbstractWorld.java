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

    public static final int eCommandHandlerMissing = 1;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Command.class, Zeze.Builtin.World.Command.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Command::new;
            factoryHandle.Handle = this::ProcessCommand;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommand", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommand", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47378281792093L, factoryHandle); // 11031, 497549917
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.EnterConfirm.class, Zeze.Builtin.World.EnterConfirm.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.EnterConfirm::new;
            factoryHandle.Handle = this::ProcessEnterConfirm;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEnterConfirm", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEnterConfirm", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47378620875273L, factoryHandle); // 11031, 836633097
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Query.class, Zeze.Builtin.World.Query.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Query::new;
            factoryHandle.Handle = this::ProcessQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47381630294274L, factoryHandle); // 11031, -448915198
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.SwitchWorld.class, Zeze.Builtin.World.SwitchWorld.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.SwitchWorld::new;
            factoryHandle.Handle = this::ProcessSwitchWorld;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSwitchWorld", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSwitchWorld", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47379342875131L, factoryHandle); // 11031, 1558632955
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47378281792093L);
        service.getFactorys().remove(47378620875273L);
        service.getFactorys().remove(47381630294274L);
        service.getFactorys().remove(47379342875131L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCommand(Zeze.Builtin.World.Command p) throws Exception;
    protected abstract long ProcessEnterConfirm(Zeze.Builtin.World.EnterConfirm p) throws Exception;
    protected abstract long ProcessQueryRequest(Zeze.Builtin.World.Query r) throws Exception;
    protected abstract long ProcessSwitchWorld(Zeze.Builtin.World.SwitchWorld p) throws Exception;
}
