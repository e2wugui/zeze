// auto-generated @formatter:off
package Zeze.Beans.Game.Online;

public class ReliableNotifyConfirm extends Zeze.Net.Rpc<Zeze.Beans.Game.Online.BReliableNotifyConfirm, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = -1455146603;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReliableNotifyConfirm() {
        Argument = new Zeze.Beans.Game.Online.BReliableNotifyConfirm();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
