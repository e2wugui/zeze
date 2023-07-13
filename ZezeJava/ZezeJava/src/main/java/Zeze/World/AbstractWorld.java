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

    protected final Zeze.Builtin.World.tLoad _tLoad = new Zeze.Builtin.World.tLoad();

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.World.Query.class, Zeze.Builtin.World.Query.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.World.Query::new;
            factoryHandle.Handle = this::ProcessQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47381630294274L, factoryHandle); // 11031, -448915198
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47378281792093L);
        service.getFactorys().remove(47381630294274L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tLoad.getName()).getDatabaseName(), _tLoad);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tLoad.getName()).getDatabaseName(), _tLoad);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCommand(Zeze.Builtin.World.Command p) throws Exception;
    protected abstract long ProcessQueryRequest(Zeze.Builtin.World.Query r) throws Exception;
}
