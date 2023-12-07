// auto-generated @formatter:off
package Zeze.Builtin.Onz;

// 两个rpc参数虽然完全一样，但开启的逻辑操作不同。
public class FuncProcedure extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BFuncProcedure.Data, Zeze.Builtin.Onz.BFuncProcedureResult.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -1471731108; // 2823236188
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47410672249436
    static { register(TypeId_, FuncProcedure.class); }

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

    public FuncProcedure() {
        Argument = new Zeze.Builtin.Onz.BFuncProcedure.Data();
        Result = new Zeze.Builtin.Onz.BFuncProcedureResult.Data();
    }

    public FuncProcedure(Zeze.Builtin.Onz.BFuncProcedure.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Onz.BFuncProcedureResult.Data();
    }
}
