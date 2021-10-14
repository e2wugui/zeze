// auto-generated
package Game.Bag;

public class CUse extends Zeze.Net.Protocol1<Game.Bag.BUse> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 52931;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CUse() {
        Argument = new Game.Bag.BUse();
    }

}
