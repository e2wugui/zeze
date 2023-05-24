// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Bind extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BBind.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 114259622;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47279114253990
    static { register(TypeId_, Bind.class); }

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

    public Bind() {
        Argument = new Zeze.Builtin.Provider.BBind.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Bind(Zeze.Builtin.Provider.BBind.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
