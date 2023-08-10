// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 发布topic. 回复的resultCode大于等于0表示已通知订阅者数
public class PubTopic extends Zeze.Net.Rpc<Zeze.Builtin.Token.BPubTopic.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = -485989023; // 3808978273
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47373003285857
    static { register(TypeId_, PubTopic.class); }

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

    public PubTopic() {
        Argument = new Zeze.Builtin.Token.BPubTopic.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public PubTopic(Zeze.Builtin.Token.BPubTopic.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
