// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class PublishSplitBucketOld extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBucketMeta.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1486365766; // 2808601530
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363412974522
    static { register(TypeId_, PublishSplitBucketOld.class); }

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

    public PublishSplitBucketOld() {
        Argument = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public PublishSplitBucketOld(Zeze.Builtin.Dbh2.BBucketMeta.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
