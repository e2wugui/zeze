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

    public static final int ePartition = 1; // 分区数或分区索引无效
    public static final int eTopicNotExist = 2; // 找不到指定主题
    public static final int eManagerNotFound = 3; // Master处理ReportLoad时找不到所属的Manager上下文
    public static final int eTopicExist = 4; // 指定的主题已存在,无法再次CreateMQ
    public static final int eConsumerNotFound = 5; // Agent收到PushMessage时找不到所属的MQConsumer上下文
    public static final int eCreatePartition = 6; // Master向Manager请求CreatePartition失败
    public static final int eTopicHasReserveChar = 7; // 指定的主题含有非法字符

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.CreateMQ.class, Zeze.Builtin.MQ.Master.CreateMQ.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.CreateMQ::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreateMQResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreateMQResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47420243782922L, factoryHandle); // 11040, -490132214
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.CreatePartition.class, Zeze.Builtin.MQ.Master.CreatePartition.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.CreatePartition::new;
            factoryHandle.Handle = this::ProcessCreatePartitionRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCreatePartitionRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCreatePartitionRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47418254762936L, factoryHandle); // 11040, 1815815096
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.Subscribe.class, Zeze.Builtin.MQ.Master.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47418979135861L, factoryHandle); // 11040, -1754779275
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.Register.class, Zeze.Builtin.MQ.Master.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.Register::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47417719098028L, factoryHandle); // 11040, 1280150188
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.ReportLoad.class, Zeze.Builtin.MQ.Master.ReportLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.ReportLoad::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoadResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoadResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47416592360823L, factoryHandle); // 11040, 153412983
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47419582250441L);
        service.getFactorys().remove(47420243782922L);
        service.getFactorys().remove(47418254762936L);
        service.getFactorys().remove(47418979135861L);
        service.getFactorys().remove(47417719098028L);
        service.getFactorys().remove(47416592360823L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessCreatePartitionRequest(Zeze.Builtin.MQ.Master.CreatePartition r) throws Exception;
}
