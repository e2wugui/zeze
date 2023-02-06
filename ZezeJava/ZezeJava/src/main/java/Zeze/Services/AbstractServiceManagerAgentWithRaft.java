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

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.AllocateId>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.AllocateId::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAllocateIdResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessAllocateIdResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342648206403L, factoryHandle); // 11022, -776297405
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList::new;
            factoryHandle.Handle = this::ProcessCommitServiceListRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessCommitServiceListRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessCommitServiceListRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340049712890L, factoryHandle); // 11022, 920176378
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.KeepAlive>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.KeepAlive::new;
            factoryHandle.Handle = this::ProcessKeepAliveRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKeepAliveRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessKeepAliveRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341226054794L, factoryHandle); // 11022, 2096518282
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Login>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Login::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessLoginResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessLoginResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339747890828L, factoryHandle); // 11022, 618354316
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.NormalClose>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.NormalClose::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNormalCloseResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNormalCloseResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342647871189L, factoryHandle); // 11022, -776632619
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList::new;
            factoryHandle.Handle = this::ProcessNotifyServiceListRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNotifyServiceListRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNotifyServiceListRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339587192283L, factoryHandle); // 11022, 457655771
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify::new;
            factoryHandle.Handle = this::ProcessOfflineNotifyRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineNotifyRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineNotifyRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340558537840L, factoryHandle); // 11022, 1429001328
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.OfflineRegister::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessOfflineRegisterResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessOfflineRegisterResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340511174741L, factoryHandle); // 11022, 1381638229
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.ReadyServiceList::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReadyServiceListResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReadyServiceListResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339697966893L, factoryHandle); // 11022, 568430381
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Register>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Register::new;
            factoryHandle.Handle = this::ProcessRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340640775066L, factoryHandle); // 11022, 1511238554
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad::new;
            factoryHandle.Handle = this::ProcessSetServerLoadRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetServerLoadRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSetServerLoadRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342529828679L, factoryHandle); // 11022, -894675129
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Subscribe>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Subscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340271484727L, factoryHandle); // 11022, 1141948215
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit::new;
            factoryHandle.Handle = this::ProcessSubscribeFirstCommitRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeFirstCommitRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubscribeFirstCommitRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47342788372847L, factoryHandle); // 11022, -636130961
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.UnRegister>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnRegister::new;
            factoryHandle.Handle = this::ProcessUnRegisterRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnRegisterRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnRegisterRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47341011400112L, factoryHandle); // 11022, 1881863600
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.UnSubscribe::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnSubscribeResponse", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnSubscribeResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47339752276364L, factoryHandle); // 11022, 622739852
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Builtin.ServiceManagerWithRaft.Update>();
            factoryHandle.Factory = Zeze.Builtin.ServiceManagerWithRaft.Update::new;
            factoryHandle.Handle = this::ProcessUpdateRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUpdateRequest", Zeze.Transaction.TransactionLevel.Serializable);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUpdateRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47340940316449L, factoryHandle); // 11022, 1810779937
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47342648206403L);
        service.getFactorys().remove(47340049712890L);
        service.getFactorys().remove(47341226054794L);
        service.getFactorys().remove(47339747890828L);
        service.getFactorys().remove(47342647871189L);
        service.getFactorys().remove(47339587192283L);
        service.getFactorys().remove(47340558537840L);
        service.getFactorys().remove(47340511174741L);
        service.getFactorys().remove(47339697966893L);
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
    }

    public void RegisterHttpServlet(Zeze.Netty.HttpServer httpServer) {
    }


    protected abstract long ProcessCommitServiceListRequest(Zeze.Builtin.ServiceManagerWithRaft.CommitServiceList r) throws Exception;
    protected abstract long ProcessKeepAliveRequest(Zeze.Builtin.ServiceManagerWithRaft.KeepAlive r) throws Exception;
    protected abstract long ProcessNotifyServiceListRequest(Zeze.Builtin.ServiceManagerWithRaft.NotifyServiceList r) throws Exception;
    protected abstract long ProcessOfflineNotifyRequest(Zeze.Builtin.ServiceManagerWithRaft.OfflineNotify r) throws Exception;
    protected abstract long ProcessRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.Register r) throws Exception;
    protected abstract long ProcessSetServerLoadRequest(Zeze.Builtin.ServiceManagerWithRaft.SetServerLoad r) throws Exception;
    protected abstract long ProcessSubscribeFirstCommitRequest(Zeze.Builtin.ServiceManagerWithRaft.SubscribeFirstCommit r) throws Exception;
    protected abstract long ProcessUnRegisterRequest(Zeze.Builtin.ServiceManagerWithRaft.UnRegister r) throws Exception;
    protected abstract long ProcessUpdateRequest(Zeze.Builtin.ServiceManagerWithRaft.Update r) throws Exception;
}
