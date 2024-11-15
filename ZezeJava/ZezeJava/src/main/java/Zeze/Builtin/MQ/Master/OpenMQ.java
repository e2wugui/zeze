// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public class OpenMQ extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = -1151664695; // 3143302601
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47419582250441
    static { register(TypeId_, OpenMQ.class); }

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

    public OpenMQ() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
