// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class FuncSaga extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BFuncSaga.Data, Zeze.Builtin.Onz.BFuncSagaResult.Data> {
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
        Argument = new Zeze.Builtin.Onz.BFuncSaga.Data();
        Result = new Zeze.Builtin.Onz.BFuncSagaResult.Data();
    }

    public FuncSaga(Zeze.Builtin.Onz.BFuncSaga.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Onz.BFuncSagaResult.Data();
    }
}
