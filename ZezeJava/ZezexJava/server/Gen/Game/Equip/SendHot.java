// auto-generated @formatter:off
package Game.Equip;

public class SendHot extends Zeze.Net.Rpc<Game.Equip.BEquipExtra, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 7;
    public static final int ProtocolId_ = 441911488;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 30506682560
    static { register(TypeId_, SendHot.class); }

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

    public SendHot() {
        Argument = new Game.Equip.BEquipExtra();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public SendHot(Game.Equip.BEquipExtra arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
