// auto-generated @formatter:off
package ClientGame.Equip;

public class SEquipement extends Zeze.Net.Protocol<ClientGame.Equip.BChangedResult> {
    public static final int ModuleId_ = 7;
    public static final int ProtocolId_ = 1231387531;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 31296158603
    static { register(TypeId_, SEquipement.class); }

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

    public SEquipement() {
        Argument = new ClientGame.Equip.BChangedResult();
    }

    public SEquipement(ClientGame.Equip.BChangedResult arg) {
        Argument = arg;
    }
}
