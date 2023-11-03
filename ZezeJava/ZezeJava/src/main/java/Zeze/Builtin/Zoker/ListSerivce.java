// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class ListSerivce extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Builtin.Zoker.BListServiceResult.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = 528002937;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47404082048889
    static { register(TypeId_, ListSerivce.class); }

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

    public ListSerivce() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = new Zeze.Builtin.Zoker.BListServiceResult.Data();
    }
}
