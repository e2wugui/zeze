// auto-generated @formatter:off
package Zeze.Component;

public abstract class AbstractProviderDistribute {
    public static final int ErrorTransmitParameterFactoryNotFound = 1;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(this.getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.AnnounceProviderInfo>();
            factoryHandle.Factory = Zeze.Beans.Provider.AnnounceProviderInfo::new;
            factoryHandle.Handle = this::ProcessAnnounceProviderInfo;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessAnnounceProviderInfo", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47281670848105L, factoryHandle); // 11008, -1624113559
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Bind>();
            factoryHandle.Factory = Zeze.Beans.Provider.Bind::new;
            factoryHandle.Handle = this::ProcessBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282301515237L, factoryHandle); // 11008, -993446427
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Broadcast>();
            factoryHandle.Factory = Zeze.Beans.Provider.Broadcast::new;
            factoryHandle.Handle = this::ProcessBroadcast;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBroadcast", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282243906435L, factoryHandle); // 11008, -1051055229
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Kick>();
            factoryHandle.Factory = Zeze.Beans.Provider.Kick::new;
            factoryHandle.Handle = this::ProcessKick;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessKick", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282516612067L, factoryHandle); // 11008, -778349597
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.ModuleRedirect>();
            factoryHandle.Factory = Zeze.Beans.Provider.ModuleRedirect::new;
            factoryHandle.Handle = this::ProcessModuleRedirectRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282751256958L, factoryHandle); // 11008, -543704706
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.ModuleRedirectAllRequest>();
            factoryHandle.Factory = Zeze.Beans.Provider.ModuleRedirectAllRequest::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280242920172L, factoryHandle); // 11008, 1242925804
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.ModuleRedirectAllResult>();
            factoryHandle.Factory = Zeze.Beans.Provider.ModuleRedirectAllResult::new;
            factoryHandle.Handle = this::ProcessModuleRedirectAllResult;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessModuleRedirectAllResult", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281313619019L, factoryHandle); // 11008, -1981342645
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.ReportLoad>();
            factoryHandle.Factory = Zeze.Beans.Provider.ReportLoad::new;
            factoryHandle.Handle = this::ProcessReportLoad;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReportLoad", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282765597827L, factoryHandle); // 11008, -529363837
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Send>();
            factoryHandle.Factory = Zeze.Beans.Provider.Send::new;
            factoryHandle.Handle = this::ProcessSend;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSend", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280423652415L, factoryHandle); // 11008, 1423658047
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.SetUserState>();
            factoryHandle.Factory = Zeze.Beans.Provider.SetUserState::new;
            factoryHandle.Handle = this::ProcessSetUserState;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSetUserState", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47281174282091L, factoryHandle); // 11008, -2120679573
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Subscribe>();
            factoryHandle.Factory = Zeze.Beans.Provider.Subscribe::new;
            factoryHandle.Handle = this::ProcessSubscribeRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubscribeRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47282665133980L, factoryHandle); // 11008, -629827684
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.Transmit>();
            factoryHandle.Factory = Zeze.Beans.Provider.Transmit::new;
            factoryHandle.Handle = this::ProcessTransmit;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTransmit", Zeze.Transaction.TransactionLevel.None);
            service.AddFactoryHandle(47279381054462L, factoryHandle); // 11008, 381060094
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<Zeze.Beans.Provider.UnBind>();
            factoryHandle.Factory = Zeze.Beans.Provider.UnBind::new;
            factoryHandle.Handle = this::ProcessUnBindRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnBindRequest", Zeze.Transaction.TransactionLevel.Serializable);
            service.AddFactoryHandle(47280773808911L, factoryHandle); // 11008, 1773814543
        }
    }

    public void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47281670848105L);
        service.getFactorys().remove(47282301515237L);
        service.getFactorys().remove(47282243906435L);
        service.getFactorys().remove(47282516612067L);
        service.getFactorys().remove(47282751256958L);
        service.getFactorys().remove(47280242920172L);
        service.getFactorys().remove(47281313619019L);
        service.getFactorys().remove(47282765597827L);
        service.getFactorys().remove(47280423652415L);
        service.getFactorys().remove(47281174282091L);
        service.getFactorys().remove(47282665133980L);
        service.getFactorys().remove(47279381054462L);
        service.getFactorys().remove(47280773808911L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessAnnounceProviderInfo(Zeze.Beans.Provider.AnnounceProviderInfo p) throws Throwable;
    protected abstract long ProcessBindRequest(Zeze.Beans.Provider.Bind r) throws Throwable;
    protected abstract long ProcessBroadcast(Zeze.Beans.Provider.Broadcast p) throws Throwable;
    protected abstract long ProcessKick(Zeze.Beans.Provider.Kick p) throws Throwable;
    protected abstract long ProcessModuleRedirectRequest(Zeze.Beans.Provider.ModuleRedirect r) throws Throwable;
    protected abstract long ProcessModuleRedirectAllRequest(Zeze.Beans.Provider.ModuleRedirectAllRequest p) throws Throwable;
    protected abstract long ProcessModuleRedirectAllResult(Zeze.Beans.Provider.ModuleRedirectAllResult p) throws Throwable;
    protected abstract long ProcessReportLoad(Zeze.Beans.Provider.ReportLoad p) throws Throwable;
    protected abstract long ProcessSend(Zeze.Beans.Provider.Send p) throws Throwable;
    protected abstract long ProcessSetUserState(Zeze.Beans.Provider.SetUserState p) throws Throwable;
    protected abstract long ProcessSubscribeRequest(Zeze.Beans.Provider.Subscribe r) throws Throwable;
    protected abstract long ProcessTransmit(Zeze.Beans.Provider.Transmit p) throws Throwable;
    protected abstract long ProcessUnBindRequest(Zeze.Beans.Provider.UnBind r) throws Throwable;
}
