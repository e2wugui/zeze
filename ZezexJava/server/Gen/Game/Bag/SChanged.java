// auto-generated
package Game.Bag;

public class SChanged extends Zeze.Net.Protocol1<Game.Bag.BChangedResult> {
    public final static int ModuleId_ = 2;
    public final static int ProtocolId_ = 59553;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SChanged() {
        Argument = new Game.Bag.BChangedResult();
    }

}
