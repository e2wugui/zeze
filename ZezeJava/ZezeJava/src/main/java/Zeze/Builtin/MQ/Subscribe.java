// auto-generated @formatter:off
package Zeze.Builtin.MQ;

// 消息消费者向某个Manager订阅某个主题
public class Subscribe extends Zeze.Net.Rpc<Zeze.Builtin.MQ.BSubscribeConsumer.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11039;
    public static final int ProtocolId_ = 873492185;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47413017472729
    static { register(TypeId_, Subscribe.class); }

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

    public Subscribe() {
        Argument = new Zeze.Builtin.MQ.BSubscribeConsumer.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Subscribe(Zeze.Builtin.MQ.BSubscribeConsumer.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
