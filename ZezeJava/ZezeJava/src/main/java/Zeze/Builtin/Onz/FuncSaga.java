// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class FuncSaga extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BFuncProcedure.Data, Zeze.Builtin.Onz.BFuncProcedureResult.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -604206421; // 3690760875
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47411539774123
    static { register(TypeId_, FuncSaga.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public FuncSaga() {
        Argument = new Zeze.Builtin.Onz.BFuncProcedure.Data();
        Result = new Zeze.Builtin.Onz.BFuncProcedureResult.Data();
    }

    public FuncSaga(Zeze.Builtin.Onz.BFuncProcedure.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Onz.BFuncProcedureResult.Data();
    }
}
