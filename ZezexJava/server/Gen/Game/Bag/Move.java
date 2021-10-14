// auto-generated
package Game.Bag;

public class Move extends Zeze.Net.Rpc<Game.Bag.BMove, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 61837;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Move() {
        Argument = new Game.Bag.BMove();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
