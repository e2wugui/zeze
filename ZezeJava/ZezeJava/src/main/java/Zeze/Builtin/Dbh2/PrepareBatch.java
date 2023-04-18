// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

// 桶正在迁移中造成Batch中部分Key失败，整个事务失败。
public class PrepareBatch extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BPrepareBatch.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -259770762; // 4035196534
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360344602230
    static { register(TypeId_, PrepareBatch.class); }

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

    public PrepareBatch() {
        Argument = new Zeze.Builtin.Dbh2.BPrepareBatch.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public PrepareBatch(Zeze.Builtin.Dbh2.BPrepareBatch.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
