// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class SetDbh2Ready extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 2087798985;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47362692171977
    static { register(TypeId_, SetDbh2Ready.class); }

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

    public SetDbh2Ready() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
