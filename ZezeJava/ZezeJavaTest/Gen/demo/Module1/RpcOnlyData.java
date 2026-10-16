// auto-generated @formatter:off
package demo.Module1;

public class RpcOnlyData extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, demo.Module1.BOnlyData> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -1253772235; // 3041195061
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 7336162357
    static { register(TypeId_, RpcOnlyData.class); }

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

    public RpcOnlyData() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = new demo.Module1.BOnlyData();
    }
}
