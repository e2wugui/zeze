// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractServiceManagerAgentWithRaft extends Zeze.Services.ServiceManager.AbstractAgent implements Zeze.IModule {
    public static final int ModuleId = 11022;
    public static final String ModuleName = "ServiceManagerAgentWithRaft";
    public static final String ModuleFullName = "Zeze.Services.ServiceManagerAgentWithRaft";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.AllocateId.class, Zeze.Builtin.ServiceManagerWithRaft.AllocateId.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.AllocateId::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAllocateIdResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAllocateIdResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342648206403L, factoryHandle); // 11022, -776297405
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.Edit.class, Zeze.Builtin.ServiceManagerWithRaft.Edit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Edit::new;
            factoryHandle.Handle = this::ProcessEditRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessEditRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessEditRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340950705715L, factoryHandle); // 11022, 1821169203
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.KeepAlive.class, Zeze.Builtin.ServiceManagerWithRaft.KeepAlive.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.KeepAlive::new;
            factoryHandle.Handle = this::ProcessKeepAliveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341226054794L, factoryHandle); // 11022, 2096518282
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.Login.class, Zeze.Builtin.ServiceManagerWithRaft.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Login::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339747890828L, factoryHandle); // 11022, 618354316
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.NormalClose.class, Zeze.Builtin.ServiceManagerWithRaft.NormalClose.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.NormalClose::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNormalCloseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342647871189L, factoryHandle); // 11022, -776632619
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify.class, Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify::new;
            factoryHandle.Handle = this::ProcessOfflineNotifyRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineNotifyRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineNotifyRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340558537840L, factoryHandle); // 11022, 1429001328
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister.class, Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineRegisterResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineRegisterResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340511174741L, factoryHandle); // 11022, 1381638229
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad.class, Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad::new;
            factoryHandle.Handle = this::ProcessSetServerLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetServerLoadRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetServerLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342529828679L, factoryHandle); // 11022, -894675129
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.Subscribe.class, Zeze.Builtin.ServiceManagerWithRaft.Subscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340271484727L, factoryHandle); // 11022, 1141948215
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe.class, Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339752276364L, factoryHandle); // 11022, 622739852
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47342648206403L);
        service.getFactorys().remove(47340950705715L);
        service.getFactorys().remove(47341226054794L);
        service.getFactorys().remove(47339747890828L);
        service.getFactorys().remove(47342647871189L);
        service.getFactorys().remove(47340558537840L);
        service.getFactorys().remove(47340511174741L);
        service.getFactorys().remove(47342529828679L);
        service.getFactorys().remove(47340271484727L);
        service.getFactorys().remove(47339752276364L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessEditRequest(Zeze.Builtin.ServiceManagerWithRaft.Edit r) throws Exception;
    protected abstract long ProcessKeepAliveRequest(Zeze.Builtin.ServiceManagerWithRaft.KeepAlive r) throws Exception;
    protected abstract long ProcessOfflineNotifyRequest(Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify r) throws Exception;
    protected abstract long ProcessSetServerLoadRequest(Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad r) throws Exception;
}
