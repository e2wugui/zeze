// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class Checkpoint extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -285492076; // 4009475220
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47411858488468
    static { register(TypeId_, Checkpoint.class); }

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

    public Checkpoint() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
