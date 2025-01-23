// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public class CreatePartition extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BCreatePartition.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = 1815815096;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47418254762936
    static { register(TypeId_, CreatePartition.class); }

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

    public CreatePartition() {
        Argument = new Zeze.Builtin.MQ.Master.BCreatePartition.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public CreatePartition(Zeze.Builtin.MQ.Master.BCreatePartition.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
