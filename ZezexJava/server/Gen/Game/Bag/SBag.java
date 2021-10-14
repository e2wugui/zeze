// auto-generated
package Game.Bag;

public class SBag extends Zeze.Net.Protocol1<Game.Bag.BBag> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 2835;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SBag() {
        Argument = new Game.Bag.BBag();
    }

}
