// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 取消订阅topic. 回复的resultCode=0:成功取消订阅; 1:之前未订阅
public class UnsubTopic extends Zeze.Net.Rpc<Zeze.Builtin.Token.BTopic.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = 1032019815;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47370226327399
    static { register(TypeId_, UnsubTopic.class); }

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

    public UnsubTopic() {
        Argument = new Zeze.Builtin.Token.BTopic.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public UnsubTopic(Zeze.Builtin.Token.BTopic.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
