// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 订阅topic. 回复的resultCode=0:成功订阅; 1:之前已订阅
public class SubTopic extends Zeze.Net.Rpc<Zeze.Builtin.Token.BTopic.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = 1314651151;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47370508958735
    static { register(TypeId_, SubTopic.class); }

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

    public SubTopic() {
        Argument = new Zeze.Builtin.Token.BTopic.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SubTopic(Zeze.Builtin.Token.BTopic.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
