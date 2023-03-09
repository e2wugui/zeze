// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

// 桶(raftNode)创建出来的第一条操作，以后分桶时也需要重新设置
public class SetBucketMeta extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BBucketMetaDaTa, Zeze.Transaction.EmptyBeanDaTa> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = 600141951;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47356909547647

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

    public SetBucketMeta() {
        Argument = new Zeze.Builtin.Dbh2.BBucketMetaDaTa();
        Result = Zeze.Transaction.EmptyBeanDaTa.instance;
    }

    public SetBucketMeta(Zeze.Builtin.Dbh2.BBucketMetaDaTa arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBeanDaTa.instance;
    }
}
