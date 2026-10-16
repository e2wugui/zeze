// auto-generated @formatter:off
package demo.Module1;

public class RpcOnlyUseData extends Zeze.Net.Rpc<demo.Module1.BOnlyData, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -651743921; // 3643223375
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 7938190671
    static { register(TypeId_, RpcOnlyUseData.class); }

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

    public RpcOnlyUseData() {
        Argument = new demo.Module1.BOnlyData();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public RpcOnlyUseData(demo.Module1.BOnlyData arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
