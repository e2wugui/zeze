// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

// Master对账裁决孤儿分区后向Manager下发删除
public class DeletePartition extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BDeletePartition.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = -441160352; // 3853806944
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47420292754784
    static { register(TypeId_, DeletePartition.class); }

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

    public DeletePartition() {
        Argument = new Zeze.Builtin.MQ.Master.BDeletePartition.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public DeletePartition(Zeze.Builtin.MQ.Master.BDeletePartition.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
