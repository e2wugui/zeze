// auto-generated @formatter:off
package Zeze.Builtin.Online;

public class ReliableNotifyConfirm extends Zeze.Net.Rpc<Zeze.Builtin.Online.BReliableNotifyConfirm, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = -244732886; // 4050234410
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47678187220010
    static { register(TypeId_, ReliableNotifyConfirm.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public ReliableNotifyConfirm() {
        Argument = new Zeze.Builtin.Online.BReliableNotifyConfirm();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public ReliableNotifyConfirm(Zeze.Builtin.Online.BReliableNotifyConfirm arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
