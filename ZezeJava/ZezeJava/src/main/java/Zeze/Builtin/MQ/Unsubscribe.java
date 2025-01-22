// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public class Unsubscribe extends Zeze.Net.Rpc<Zeze.Builtin.MQ.BSubscribeConsumer.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11039;
    public static final int ProtocolId_ = 229847595;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47412373828139
    static { register(TypeId_, Unsubscribe.class); }

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

    public Unsubscribe() {
        Argument = new Zeze.Builtin.MQ.BSubscribeConsumer.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Unsubscribe(Zeze.Builtin.MQ.BSubscribeConsumer.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
