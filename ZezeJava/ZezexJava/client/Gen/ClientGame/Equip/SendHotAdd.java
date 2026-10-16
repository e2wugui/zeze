// auto-generated @formatter:off
package ClientGame.Equip;

public class SendHotAdd extends Zeze.Net.Rpc<ClientGame.Equip.BEquipExtra, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 7;
    public static final int ProtocolId_ = 251336778;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 30316107850
    static { register(TypeId_, SendHotAdd.class); }

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

    public SendHotAdd() {
        Argument = new ClientGame.Equip.BEquipExtra();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public SendHotAdd(ClientGame.Equip.BEquipExtra arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
