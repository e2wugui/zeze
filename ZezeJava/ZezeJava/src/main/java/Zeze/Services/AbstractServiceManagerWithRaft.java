// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractServiceManagerWithRaft implements Zeze.IModule {
    public static final int ModuleId = 11022;
    public static final String ModuleName = "ServiceManagerWithRaft";
    public static final String ModuleFullName = "Zeze.Services.ServiceManagerWithRaft";

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
            factoryHandle.Handle = this::ProcessAllocateIdRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAllocateIdRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAllocateIdRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342648206403L, factoryHandle); // 11022, -776297405
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.KeepAlive.class, Zeze.Builtin.ServiceManagerWithRaft.KeepAlive.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.KeepAlive::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341226054794L, factoryHandle); // 11022, 2096518282
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.Login.class, Zeze.Builtin.ServiceManagerWithRaft.Login.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Login::new;
            factoryHandle.Handle = this::ProcessLoginRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339747890828L, factoryHandle); // 11022, 618354316
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.NormalClose.class, Zeze.Builtin.ServiceManagerWithRaft.NormalClose.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.NormalClose::new;
            factoryHandle.Handle = this::ProcessNormalCloseRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNormalCloseRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342647871189L, factoryHandle); // 11022, -776632619
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify.class, Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineNotifyResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineNotifyResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340558537840L, factoryHandle); // 11022, 1429001328
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister.class, Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister::new;
            factoryHandle.Handle = this::ProcessOfflineRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340511174741L, factoryHandle); // 11022, 1381638229
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.Register.class, Zeze.Builtin.ServiceManagerWithRaft.Register.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340640775066L, factoryHandle); // 11022, 1511238554
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
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340271484727L, factoryHandle); // 11022, 1141948215
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit.class, Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeFirstCommitResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeFirstCommitResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342788372847L, factoryHandle); // 11022, -636130961
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.UnRegister.class, Zeze.Builtin.ServiceManagerWithRaft.UnRegister.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnRegister::new;
            factoryHandle.Handle = this::ProcessUnRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341011400112L, factoryHandle); // 11022, 1881863600
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe.class, Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe::new;
            factoryHandle.Handle = this::ProcessUnSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnSubscribeRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339752276364L, factoryHandle); // 11022, 622739852
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.ServiceManagerWithRaft.Update.class, Zeze.Builtin.ServiceManagerWithRaft.Update.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Update::new;
            factoryHandle.Handle = this::ProcessUpdateRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUpdateRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUpdateRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340940316449L, factoryHandle); // 11022, 1810779937
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47342648206403L);
        service.getFactorys().remove(47341226054794L);
        service.getFactorys().remove(47339747890828L);
        service.getFactorys().remove(47342647871189L);
        service.getFactorys().remove(47340558537840L);
        service.getFactorys().remove(47340511174741L);
        service.getFactorys().remove(47340640775066L);
        service.getFactorys().remove(47342529828679L);
        service.getFactorys().remove(47340271484727L);
        service.getFactorys().remove(47342788372847L);
        service.getFactorys().remove(47341011400112L);
        service.getFactorys().remove(47339752276364L);
        service.getFactorys().remove(47340940316449L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
        rocks.registerTableTemplate("tAutoKey", String.class, Zeze.Builtin.ServiceManagerWithRaft.BAutoKey.class);
        rocks.registerTableTemplate("tLoadObservers", String.class, Zeze.Builtin.ServiceManagerWithRaft.BLoadObservers.class);
        rocks.registerTableTemplate("tServerState", String.class, Zeze.Builtin.ServiceManagerWithRaft.BServerState.class);
        rocks.registerTableTemplate("tSession", String.class, Zeze.Builtin.ServiceManagerWithRaft.BSession.class);
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogSet1<>(String.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeStateRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotifyRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap2<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks.class, Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.Log1.LogBeanKey<>(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks.class));
        Zeze.Raft.RocksRaft.Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogMap2<>(String.class, Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfoRocks.class));
    }

    protected abstract long ProcessAllocateIdRequest(Zeze.Builtin.ServiceManagerWithRaft.AllocateId r) throws Exception;
    protected abstract long ProcessLoginRequest(Zeze.Builtin.ServiceManagerWithRaft.Login r) throws Exception;
    protected abstract long ProcessNormalCloseRequest(Zeze.Builtin.ServiceManagerWithRaft.NormalClose r) throws Exception;
    protected abstract long ProcessOfflineRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister r) throws Exception;
    protected abstract long ProcessRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.Register r) throws Exception;
    protected abstract long ProcessSetServerLoadRequest(Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad r) throws Exception;
    protected abstract long ProcessSubscribeRequest(Zeze.Builtin.ServiceManagerWithRaft.Subscribe r) throws Exception;
    protected abstract long ProcessUnRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.UnRegister r) throws Exception;
    protected abstract long ProcessUnSubscribeRequest(Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe r) throws Exception;
    protected abstract long ProcessUpdateRequest(Zeze.Builtin.ServiceManagerWithRaft.Update r) throws Exception;
}
