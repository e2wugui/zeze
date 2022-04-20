// auto-generated @formatter:off
package Zeze.Beans.Game.Online;

public class Login extends Zeze.Net.Rpc<Zeze.Beans.Game.Online.BLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = 311370305;
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
        Argument = new Zeze.Beans.Game.Online.BLogin();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
