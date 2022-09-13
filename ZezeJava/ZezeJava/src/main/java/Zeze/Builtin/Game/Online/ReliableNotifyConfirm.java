// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public class ReliableNotifyConfirm extends Zeze.Net.Rpc<Zeze.Builtin.Game.Online.BReliableNotifyConfirm, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = -420042484; // 3874924812
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47304349755660

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReliableNotifyConfirm() {
        Argument = new Zeze.Builtin.Game.Online.BReliableNotifyConfirm();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public ReliableNotifyConfirm(Zeze.Builtin.Game.Online.BReliableNotifyConfirm arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
