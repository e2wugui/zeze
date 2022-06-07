// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public class ReLogin extends Zeze.Net.Rpc<Zeze.Builtin.Game.Online.BReLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = -218681811; // 4076285485
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47304551116333

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReLogin() {
        Argument = new Zeze.Builtin.Game.Online.BReLogin();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
