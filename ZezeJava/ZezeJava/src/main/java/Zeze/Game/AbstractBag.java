// auto-generated @formatter:off
package Zeze.Game;

public abstract class AbstractBag extends Zeze.IModule {
    @Override public String getFullName() { return "Zeze.Beans.Game.Bag"; }
    @Override public String getName() { return "Bag"; }
    @Override public int getId() { return ModuleId; }
    public static final int ModuleId = 11014;

    public static final int ResultCodeFromInvalid = 1;
    public static final int ResultCodeToInvalid = 2;
    public static final int ResultCodeFromNotExist = 3;
    public static final int ResultCodeTrySplitButTargetExistDifferenceItem = 4;

    protected final Zeze.Beans.Game.Bag.tbag _tbag = new Zeze.Beans.Game.Bag.tbag();
    protected final Zeze.Beans.Game.Bag.tItemClasses _tItemClasses = new Zeze.Beans.Game.Bag.tItemClasses();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Game.Bag.Destroy>();
            factoryHandle.Factory = Zeze.Beans.Game.Bag.Destroy::new;
            factoryHandle.Handle = this::ProcessDestroyRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessDestroyRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47305007008671L, factoryHandle); // 11014, 237210527
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Game.Bag.Move>();
            factoryHandle.Factory = Zeze.Beans.Game.Bag.Move::new;
            factoryHandle.Handle = this::ProcessMoveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessMoveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47307875157850L, factoryHandle); // 11014, -1189607590
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47305007008671L);
        service.getFactorys().remove(47307875157850L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.AddTable(zeze.getConfig().GetTableConf(_tbag.getName()).getDatabaseName(), _tbag);
        zeze.AddTable(zeze.getConfig().GetTableConf(_tItemClasses.getName()).getDatabaseName(), _tItemClasses);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tbag.getName()).getDatabaseName(), _tbag);
        zeze.RemoveTable(zeze.getConfig().GetTableConf(_tItemClasses.getName()).getDatabaseName(), _tItemClasses);
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessDestroyRequest(Zeze.Beans.Game.Bag.Destroy r) throws Throwable;
    protected abstract long ProcessMoveRequest(Zeze.Beans.Game.Bag.Move r) throws Throwable;
}
