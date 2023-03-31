// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class UnBind extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BBind, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 2107584596;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281107578964
    static { register(TypeId_, UnBind.class); }

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

    public UnBind() {
        Argument = new Zeze.Builtin.Provider.BBind();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public UnBind(Zeze.Builtin.Provider.BBind arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
