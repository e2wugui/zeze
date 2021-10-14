// auto-generated
package Game.Equip;

public class Unequipement extends Zeze.Net.Rpc<Game.Equip.BUnequipement, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 7;
    public final static int ProtocolId_ = 24739;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Unequipement() {
        Argument = new Game.Equip.BUnequipement();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
