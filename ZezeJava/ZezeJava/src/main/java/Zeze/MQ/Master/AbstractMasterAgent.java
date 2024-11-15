// auto-generated @formatter:off
package Zeze.MQ.Master;

public abstract class AbstractMasterAgent implements Zeze.IModule {
    public static final int ModuleId = 11040;
    public static final String ModuleName = "MasterAgent";
    public static final String ModuleFullName = "Zeze.MQ.Master.MasterAgent";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.OpenMQ.class, Zeze.Builtin.MQ.Master.OpenMQ.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.OpenMQ::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOpenMQResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOpenMQResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47419582250441L, factoryHandle); // 11040, -1151664695
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.Subscribe.class, Zeze.Builtin.MQ.Master.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47418979135861L, factoryHandle); // 11040, -1754779275
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47419582250441L);
        service.getFactorys().remove(47418979135861L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
