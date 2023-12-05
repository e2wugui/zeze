// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class FuncSagaCancel extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BFuncSagaCancel.Data, Zeze.Builtin.Onz.BFuncSagaCancelResult.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -1627037598; // 2667929698
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47410516942946
    static { register(TypeId_, FuncSagaCancel.class); }

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

    public FuncSagaCancel() {
        Argument = new Zeze.Builtin.Onz.BFuncSagaCancel.Data();
        Result = new Zeze.Builtin.Onz.BFuncSagaCancelResult.Data();
    }

    public FuncSagaCancel(Zeze.Builtin.Onz.BFuncSagaCancel.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Onz.BFuncSagaCancelResult.Data();
    }
}
