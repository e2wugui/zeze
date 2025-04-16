// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class ListService extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Builtin.Zoker.BListServiceResult.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = 461797473;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47404015843425
    static { register(TypeId_, ListService.class); }

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

    public ListService() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = new Zeze.Builtin.Zoker.BListServiceResult.Data();
    }
}
