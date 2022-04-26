// auto-generated @formatter:off
package Zeze.Builtin.Online;

public class Login extends Zeze.Net.Rpc<Zeze.Builtin.Online.BLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = -1498951762;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Login() {
        Argument = new Zeze.Builtin.Online.BLogin();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
