// auto-generated
package Game.Bag;

public class Sort extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 11000;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Sort() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
