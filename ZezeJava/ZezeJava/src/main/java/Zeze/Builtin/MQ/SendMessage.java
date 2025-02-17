// auto-generated @formatter:off
package Zeze.Builtin.MQ;

// 消息生产者向某个Manager发送消息
public class SendMessage extends Zeze.Net.Rpc<Zeze.Builtin.MQ.BSendMessage.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11039;
    public static final int ProtocolId_ = -944163063; // 3350804233
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47415494784777
    static { register(TypeId_, SendMessage.class); }

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

    public SendMessage() {
        Argument = new Zeze.Builtin.MQ.BSendMessage.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SendMessage(Zeze.Builtin.MQ.BSendMessage.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
