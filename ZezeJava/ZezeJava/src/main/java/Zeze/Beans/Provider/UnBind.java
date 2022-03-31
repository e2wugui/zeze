// auto-generated @formatter:off
package Zeze.Beans.Provider;

public class UnBind extends Zeze.Net.Rpc<Zeze.Beans.Provider.BBind, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 1773814543;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public UnBind() {
        Argument = new Zeze.Beans.Provider.BBind();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
