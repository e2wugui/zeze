// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Bind extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BBind, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 114259622;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47279114253990

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Bind() {
        Argument = new Zeze.Builtin.Provider.BBind();
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
