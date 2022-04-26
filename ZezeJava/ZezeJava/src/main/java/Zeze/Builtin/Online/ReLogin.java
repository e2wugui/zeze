// auto-generated @formatter:off
package Zeze.Builtin.Online;

public class ReLogin extends Zeze.Net.Rpc<Zeze.Builtin.Online.BReLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = 927898915;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReLogin() {
        Argument = new Zeze.Builtin.Online.BReLogin();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
