// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public class Logout extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = -563842687; // 3731124609
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47304205955457

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Logout() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
