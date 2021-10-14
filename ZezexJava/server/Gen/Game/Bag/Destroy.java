// auto-generated
package Game.Bag;

public class Destroy extends Zeze.Net.Rpc<Game.Bag.BDestroy, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 43966;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Destroy() {
        Argument = new Game.Bag.BDestroy();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
