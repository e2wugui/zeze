// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Subscribe extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BSubscribe, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 1110460218;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Subscribe() {
        Argument = new Zeze.Builtin.Provider.BSubscribe();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
