// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class PublishSplitBucketNew extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBucketMeta.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 206892825;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360811265817
    static { register(TypeId_, PublishSplitBucketNew.class); }

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

    public PublishSplitBucketNew() {
        Argument = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public PublishSplitBucketNew(Zeze.Builtin.Dbh2.BBucketMeta.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
