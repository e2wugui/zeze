// auto-generated @formatter:off
package Zeze.Builtin.Online;

public class ReliableNotifyConfirm extends Zeze.Net.Rpc<Zeze.Builtin.Online.BReliableNotifyConfirm, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = -244732886;
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
        Argument = new Zeze.Builtin.Online.BReliableNotifyConfirm();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
