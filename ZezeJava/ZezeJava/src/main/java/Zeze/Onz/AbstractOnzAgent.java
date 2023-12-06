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

    public static final int eProcedureNotFound = 1;
    public static final int eSagaNotFound = 2;
    public static final int eSagaTidExist = 3;
    public static final int eOnzTidNotFound = 4;
    public static final int eFlushAsync = 1;
    public static final int eFlushImmediately = 2;

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FlushReady.class, Zeze.Builtin.Onz.FlushReady.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FlushReady::new;
            factoryHandle.Handle = this::ProcessFlushReadyRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFlushReadyRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFlushReadyRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410000793930L, factoryHandle); // 11038, -2143186614
        }
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
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.FuncSagaEnd.class, Zeze.Builtin.Onz.FuncSagaEnd.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.FuncSagaEnd::new;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessFuncSagaEndResponse", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessFuncSagaEndResponse", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47409308020494L, factoryHandle); // 11038, 1459007246
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.Onz.Ready.class, Zeze.Builtin.Onz.Ready.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.Onz.Ready::new;
            factoryHandle.Handle = this::ProcessReadyRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessReadyRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessReadyRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47410769183880L, factoryHandle); // 11038, -1374796664
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47410000793930L);
        service.getFactorys().remove(47410672249436L);
        service.getFactorys().remove(47411539774123L);
        service.getFactorys().remove(47409308020494L);
        service.getFactorys().remove(47410769183880L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessFlushReadyRequest(Zeze.Builtin.Onz.FlushReady r) throws Exception;
    protected abstract long ProcessReadyRequest(Zeze.Builtin.Onz.Ready r) throws Exception;
}
