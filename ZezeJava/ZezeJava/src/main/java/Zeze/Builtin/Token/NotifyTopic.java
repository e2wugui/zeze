// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 通知订阅者
public class NotifyTopic extends Zeze.Net.Protocol<Zeze.Builtin.Token.BPubTopic.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = 342207101;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47369536514685
    static { register(TypeId_, NotifyTopic.class); }

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

    public NotifyTopic() {
        Argument = new Zeze.Builtin.Token.BPubTopic.Data();
    }

    public NotifyTopic(Zeze.Builtin.Token.BPubTopic.Data arg) {
        Argument = arg;
    }
}
