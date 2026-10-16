// auto-generated @formatter:off
package Game.Equip;

public class Unequipement extends Zeze.Net.Rpc<Game.Equip.BUnequipement, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 7;
    public static final int ProtocolId_ = 914970154;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 30979741226
    static { register(TypeId_, Unequipement.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public Unequipement() {
        Argument = new Game.Equip.BUnequipement();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Unequipement(Game.Equip.BUnequipement arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
