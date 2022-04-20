// auto-generated @formatter:off
package Zeze.Beans.Game.Online;

public class Logout extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = 1791022339;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Logout() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
