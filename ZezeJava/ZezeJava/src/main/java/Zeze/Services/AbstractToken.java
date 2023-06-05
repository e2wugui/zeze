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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Token.TokenStatus.class, Zeze.Builtin.Token.TokenStatus.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Token.TokenStatus::new;
            factoryHandle.Handle = this::ProcessTokenStatusRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessTokenStatusRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessTokenStatusRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47373124176530L, factoryHandle); // 11029, -365098350
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47371919073971L);
        service.getFactorys().remove(47372856131924L);
        service.getFactorys().remove(47373124176530L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessGetTokenRequest(Zeze.Builtin.Token.GetToken r) throws Exception;
    protected abstract long ProcessNewTokenRequest(Zeze.Builtin.Token.NewToken r) throws Exception;
    protected abstract long ProcessTokenStatusRequest(Zeze.Builtin.Token.TokenStatus r) throws Exception;
}
