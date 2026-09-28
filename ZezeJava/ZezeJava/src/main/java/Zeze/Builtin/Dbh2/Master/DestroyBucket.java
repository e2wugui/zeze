// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

/*
建桶半失败回滚时Master请Manager销毁刚建的raft；幂等：raft不存在也返回成功。
				argument与CreateBucket同（raftConfig为发给该manager的同款替换后配置）。
*/
public class DestroyBucket extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBucketMeta.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -764024498; // 3530942798
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47364135315790
    static { register(TypeId_, DestroyBucket.class); }

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

    public DestroyBucket() {
        Argument = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public DestroyBucket(Zeze.Builtin.Dbh2.BBucketMeta.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
