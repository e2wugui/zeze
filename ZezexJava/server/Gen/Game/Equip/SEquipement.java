// auto-generated
package Game.Equip;

public class SEquipement extends Zeze.Net.Protocol1<Game.Bag.BChangedResult> {
    public final static int ModuleId_ = 7;
    public final static int ProtocolId_ = 13038;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SEquipement() {
        Argument = new Game.Bag.BChangedResult();
    }

}
