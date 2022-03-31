// auto-generated @formatter:off
package Zeze.Beans.Provider;

public class Bind extends Zeze.Net.Rpc<Zeze.Beans.Provider.BBind, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -993446427;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Bind() {
        Argument = new Zeze.Beans.Provider.BBind();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
