// auto-generated @formatter:off
package ClientGame.Equip;

public class Equipement extends Zeze.Net.Rpc<ClientGame.Equip.BEquipement, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 7;
    public static final int ProtocolId_ = -1587253130; // 2707714166
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 32772485238
    static { register(TypeId_, Equipement.class); }

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

    public Equipement() {
        Argument = new ClientGame.Equip.BEquipement();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Equipement(ClientGame.Equip.BEquipement arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
