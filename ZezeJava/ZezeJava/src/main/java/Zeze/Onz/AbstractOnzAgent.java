// auto-generated @formatter:off
package Zeze.Onz;

public abstract class AbstractOnzAgent implements Zeze.IModule {
    public static final int ModuleId = 11038;
    public static final String ModuleName = "OnzAgent";
    public static final String ModuleFullName = "Zeze.Onz.OnzAgent";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncProcedure.class, Zeze.Builtin.Onz.FuncProcedure.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncProcedure::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncProcedureResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncProcedureResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410672249436L, factoryHandle); // 11038, -1471731108
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSaga.class, Zeze.Builtin.Onz.FuncSaga.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSaga::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47411539774123L, factoryHandle); // 11038, -604206421
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSagaCancel.class, Zeze.Builtin.Onz.FuncSagaCancel.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSagaCancel::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaCancelResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaCancelResponse", Zeze.Transaction.DispatchMode.Normal);
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
}
