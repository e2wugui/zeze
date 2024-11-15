// auto-generated @formatter:off
package Zeze.MQ;

public abstract class AbstractMQ implements Zeze.IModule {
    public static final int ModuleId = 11039;
    public static final String ModuleName = "MQ";
    public static final String ModuleFullName = "Zeze.MQ.MQ";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.SendMessage.class, Zeze.Builtin.MQ.SendMessage.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.SendMessage::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendMessageResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendMessageResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47415494784777L, factoryHandle); // 11039, -944163063
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Subscribe.class, Zeze.Builtin.MQ.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47413017472729L, factoryHandle); // 11039, 873492185
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47415494784777L);
        service.getFactorys().remove(47413017472729L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }
}
