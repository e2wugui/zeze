// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class CreateSplitBucket extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBucketMeta.Data, Zeze.Builtin.Dbh2.BBucketMeta.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 2060404378;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47362664777370
    static { register(TypeId_, CreateSplitBucket.class); }

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

    public CreateSplitBucket() {
        Argument = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
        Result = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
    }

    public CreateSplitBucket(Zeze.Builtin.Dbh2.BBucketMeta.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BBucketMeta.Data();
    }
}
