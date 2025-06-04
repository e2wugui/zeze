// auto-generated @formatter:off
package Zeze.MQ;

public abstract class AbstractMQManager implements Zeze.IModule {
    public static final int ModuleId = 11039;
    public static final String ModuleName = "MQManager";
    public static final String ModuleFullName = "Zeze.MQ.MQManager";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Subscribe.class, Zeze.Builtin.MQ.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47413017472729L, factoryHandle); // 11039, 873492185
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Unsubscribe.class, Zeze.Builtin.MQ.Unsubscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Unsubscribe::new;
            factoryHandle.Handle = this::ProcessUnsubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnsubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnsubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47412373828139L, factoryHandle); // 11039, 229847595
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.SendMessage.class, Zeze.Builtin.MQ.SendMessage.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.SendMessage::new;
            factoryHandle.Handle = this::ProcessSendMessageRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSendMessageRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSendMessageRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47415494784777L, factoryHandle); // 11039, -944163063
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.PushMessage.class, Zeze.Builtin.MQ.PushMessage.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.PushMessage::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPushMessageResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPushMessageResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47415515233719L, factoryHandle); // 11039, -923714121
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47413017472729L);
        service.getFactorys().remove(47412373828139L);
        service.getFactorys().remove(47415494784777L);
        service.getFactorys().remove(47415515233719L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.MQ.Subscribe r) throws Exception;
    protected abstract long ProcessUnsubscribeRequest(Zeze.Builtin.MQ.Unsubscribe r) throws Exception;
    protected abstract long ProcessSendMessageRequest(Zeze.Builtin.MQ.SendMessage r) throws Exception;
}
