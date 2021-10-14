// auto-generated
package Game.Equip;

public class Equipement extends Zeze.Net.Rpc<Game.Equip.BEquipement, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 7;
    public final static int ProtocolId_ = 53522;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Equipement() {
        Argument = new Game.Equip.BEquipement();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
