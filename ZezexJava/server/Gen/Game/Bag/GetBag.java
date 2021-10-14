// auto-generated
package Game.Bag;

public class GetBag extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Game.Bag.BBag> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 6367;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public GetBag() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Game.Bag.BBag();
    }

}
