// auto-generated
package Zezex.Provider;

public class UnBind extends Zeze.Net.Rpc<Zezex.Provider.BBind, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 10770;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public UnBind() {
        Argument = new Zezex.Provider.BBind();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
