// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public class PushMessage extends Zeze.Net.Rpc<Zeze.Builtin.MQ.BPushMessage.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11039;
    public static final int ProtocolId_ = -923714121; // 3371253175
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47415515233719
    static { register(TypeId_, PushMessage.class); }

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

    public PushMessage() {
        Argument = new Zeze.Builtin.MQ.BPushMessage.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public PushMessage(Zeze.Builtin.MQ.BPushMessage.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
