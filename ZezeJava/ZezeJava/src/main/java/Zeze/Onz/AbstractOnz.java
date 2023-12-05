// auto-generated @formatter:off
package Zeze.Onz;

public abstract class AbstractOnz implements Zeze.IModule {
    public static final int ModuleId = 11038;
    public static final String ModuleName = "Onz";
    public static final String ModuleFullName = "Zeze.Onz.Onz";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncProcedure.class, Zeze.Builtin.Onz.FuncProcedure.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncProcedure::new;
            factoryHandle.Handle = this::ProcessFuncProcedureRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncProcedureRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncProcedureRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410672249436L, factoryHandle); // 11038, -1471731108
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSaga.class, Zeze.Builtin.Onz.FuncSaga.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSaga::new;
            factoryHandle.Handle = this::ProcessFuncSagaRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47411539774123L, factoryHandle); // 11038, -604206421
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSagaCancel.class, Zeze.Builtin.Onz.FuncSagaCancel.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSagaCancel::new;
            factoryHandle.Handle = this::ProcessFuncSagaCancelRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaCancelRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaCancelRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410516942946L, factoryHandle); // 11038, -1627037598
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47410672249436L);
        service.getFactorys().remove(47411539774123L);
        service.getFactorys().remove(47410516942946L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) throws Exception;
    protected abstract long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception;
    protected abstract long ProcessFuncSagaCancelRequest(Zeze.Builtin.Onz.FuncSagaCancel r) throws Exception;
}
