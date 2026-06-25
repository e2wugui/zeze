// auto-generated @formatter:off
package metagame.World;

public abstract class AbstractWorld implements Zeze.IModule {
    public static final int ModuleId = 10002;
    public static final String ModuleName = "World";
    public static final String ModuleFullName = "metagame.World.World";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int eCommandHandlerMissing = 1;

    protected final metagame.builtin.World.tLoad _tLoad = new metagame.builtin.World.tLoad();

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.World.Command.class, metagame.builtin.World.Command.TypeId_);
            factoryHandle.Factory = metagame.builtin.World.Command::new;
            factoryHandle.Handle = this::ProcessCommand;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommand", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommand", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42960417406895L, factoryHandle); // 10002, -2140454993
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(metagame.builtin.World.Query.class, metagame.builtin.World.Query.TypeId_);
            factoryHandle.Factory = metagame.builtin.World.Query::new;
            factoryHandle.Handle = this::ProcessQueryRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessQueryRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessQueryRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(42960328328405L, factoryHandle); // 10002, 2065433813
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(42960417406895L);
        service.getFactorys().remove(42960328328405L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
        zeze.addTable(zeze.getConfig().getTableConf(_tLoad.getName()).getDatabaseName(), _tLoad);
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
        zeze.removeTable(zeze.getConfig().getTableConf(_tLoad.getName()).getDatabaseName(), _tLoad);
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCommand(metagame.builtin.World.Command p) throws Exception;
    protected abstract long ProcessQueryRequest(metagame.builtin.World.Query r) throws Exception;
}
