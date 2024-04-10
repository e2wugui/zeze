// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractToken implements Zeze.IModule {
    public static final int ModuleId = 11029;
    public static final String ModuleName = "Token";
    public static final String ModuleFullName = "Zeze.Services.Token";

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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.GetToken.class, Zeze.Builtin.Token.GetToken.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.GetToken::new;
            factoryHandle.Handle = this::ProcessGetTokenRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessGetTokenRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessGetTokenRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47371919073971L, factoryHandle); // 11029, -1570200909
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.NewToken.class, Zeze.Builtin.Token.NewToken.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.NewToken::new;
            factoryHandle.Handle = this::ProcessNewTokenRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessNewTokenRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessNewTokenRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47372856131924L, factoryHandle); // 11029, -633142956
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.PubTopic.class, Zeze.Builtin.Token.PubTopic.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.PubTopic::new;
            factoryHandle.Handle = this::ProcessPubTopicRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessPubTopicRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessPubTopicRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47373003285857L, factoryHandle); // 11029, -485989023
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.SubTopic.class, Zeze.Builtin.Token.SubTopic.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.SubTopic::new;
            factoryHandle.Handle = this::ProcessSubTopicRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSubTopicRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSubTopicRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47370508958735L, factoryHandle); // 11029, 1314651151
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.TokenStatus.class, Zeze.Builtin.Token.TokenStatus.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.TokenStatus::new;
            factoryHandle.Handle = this::ProcessTokenStatusRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTokenStatusRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTokenStatusRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47373124176530L, factoryHandle); // 11029, -365098350
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.UnsubTopic.class, Zeze.Builtin.Token.UnsubTopic.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.UnsubTopic::new;
            factoryHandle.Handle = this::ProcessUnsubTopicRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessUnsubTopicRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessUnsubTopicRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47370226327399L, factoryHandle); // 11029, 1032019815
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47371919073971L);
        service.getFactorys().remove(47372856131924L);
        service.getFactorys().remove(47373003285857L);
        service.getFactorys().remove(47370508958735L);
        service.getFactorys().remove(47373124176530L);
        service.getFactorys().remove(47370226327399L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessGetTokenRequest(Zeze.Builtin.Token.GetToken r) throws Exception;
    protected abstract long ProcessNewTokenRequest(Zeze.Builtin.Token.NewToken r) throws Exception;
    protected abstract long ProcessPubTopicRequest(Zeze.Builtin.Token.PubTopic r) throws Exception;
    protected abstract long ProcessSubTopicRequest(Zeze.Builtin.Token.SubTopic r) throws Exception;
    protected abstract long ProcessTokenStatusRequest(Zeze.Builtin.Token.TokenStatus r) throws Exception;
    protected abstract long ProcessUnsubTopicRequest(Zeze.Builtin.Token.UnsubTopic r) throws Exception;
}
