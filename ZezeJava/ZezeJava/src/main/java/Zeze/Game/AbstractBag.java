// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractBag implements Zeze.IModule {
    public static final int ModuleId = 11014;
    public static final String ModuleName = "Bag";
    public static final String ModuleFullName = "Zeze.Game.Bag";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public static final int ResultCodeFromInvalid = 1;
    public static final int ResultCodeToInvalid = 2;
    public static final int ResultCodeFromNotExist = 3;
    public static final int ResultCodeTrySplitButTargetExistDifferenceItem = 4;

    protected final Zeze.Builtin.Game.Bag.tbag _tbag = new Zeze.Builtin.Game.Bag.tbag();
    protected final Zeze.Builtin.Game.Bag.tItemClasses _tItemClasses = new Zeze.Builtin.Game.Bag.tItemClasses();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Bag.Destroy.class, Zeze.Builtin.Game.Bag.Destroy.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Bag.Destroy::new;
            factoryHandle.Handle = this::ProcessDestroyRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDestroyRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessDestroyRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47307869964755L, factoryHandle); // 11014, -1194800685
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Game.Bag.Move.class, Zeze.Builtin.Game.Bag.Move.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Game.Bag.Move::new;
            factoryHandle.Handle = this::ProcessMoveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessMoveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessMoveRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47308274693689L, factoryHandle); // 11014, -790071751
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47307869964755L);
        service.getFactorys().remove(47308274693689L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tbag.getName()).getDatabaseName(), _tbag);
        zeze.addTable(zeze.getConfig().getTableConf(_tItemClasses.getName()).getDatabaseName(), _tItemClasses);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tbag.getName()).getDatabaseName(), _tbag);
        zeze.removeTable(zeze.getConfig().getTableConf(_tItemClasses.getName()).getDatabaseName(), _tItemClasses);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }


    protected abstract long ProcessDestroyRequest(Zeze.Builtin.Game.Bag.Destroy r) throws Exception;
    protected abstract long ProcessMoveRequest(Zeze.Builtin.Game.Bag.Move r) throws Exception;
}
