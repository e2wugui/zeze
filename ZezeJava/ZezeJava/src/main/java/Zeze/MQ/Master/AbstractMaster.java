// auto-generated @formatter:off
package Zeze.MQ.Master;

public abstract class AbstractMaster implements Zeze.IModule {
    public static final int ModuleId = 11040;
    public static final String ModuleName = "Master";
    public static final String ModuleFullName = "Zeze.MQ.Master.Master";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    private transient final java.util.concurrent.locks.ReentrantLock __thisLock = new java.util.concurrent.locks.ReentrantLock();
    @Override public void lock() { __thisLock.lock(); }
    @Override public void unlock() { __thisLock.unlock(); }
    @Override public java.util.concurrent.locks.Lock getLock() { return __thisLock; }

    public static final int ePartition = 1;
    public static final int eTopicNotExist = 2;
    public static final int eManagerNotFound = 3;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.OpenMQ.class, Zeze.Builtin.MQ.Master.OpenMQ.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.OpenMQ::new;
            factoryHandle.Handle = this::ProcessOpenMQRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOpenMQRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOpenMQRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47419582250441L, factoryHandle); // 11040, -1151664695
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.Register.class, Zeze.Builtin.MQ.Master.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47417719098028L, factoryHandle); // 11040, 1280150188
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.ReportLoad.class, Zeze.Builtin.MQ.Master.ReportLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.ReportLoad::new;
            factoryHandle.Handle = this::ProcessReportLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoadRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReportLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47416592360823L, factoryHandle); // 11040, 153412983
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.MQ.Master.Subscribe.class, Zeze.Builtin.MQ.Master.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.MQ.Master.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47418979135861L, factoryHandle); // 11040, -1754779275
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47419582250441L);
        service.getFactorys().remove(47417719098028L);
        service.getFactorys().remove(47416592360823L);
        service.getFactorys().remove(47418979135861L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessOpenMQRequest(Zeze.Builtin.MQ.Master.OpenMQ r) throws Exception;
    protected abstract long ProcessRegisterRequest(Zeze.Builtin.MQ.Master.Register r) throws Exception;
    protected abstract long ProcessReportLoadRequest(Zeze.Builtin.MQ.Master.ReportLoad r) throws Exception;
    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.MQ.Master.Subscribe r) throws Exception;
}
