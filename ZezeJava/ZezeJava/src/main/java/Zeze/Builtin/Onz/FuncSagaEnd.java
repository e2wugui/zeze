// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class FuncSagaEnd extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BFuncSagaEnd.Data, Zeze.Builtin.Onz.BFuncSagaEndResult.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = 1459007246;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47409308020494
    static { register(TypeId_, FuncSagaEnd.class); }

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

    public FuncSagaEnd() {
        Argument = new Zeze.Builtin.Onz.BFuncSagaEnd.Data();
        Result = new Zeze.Builtin.Onz.BFuncSagaEndResult.Data();
    }

    public FuncSagaEnd(Zeze.Builtin.Onz.BFuncSagaEnd.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Onz.BFuncSagaEndResult.Data();
    }
}
