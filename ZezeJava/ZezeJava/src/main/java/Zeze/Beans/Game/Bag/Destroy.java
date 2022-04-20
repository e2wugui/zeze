// auto-generated @formatter:off
package Zeze.Beans.Game.Bag;

public class Destroy extends Zeze.Net.Rpc<Zeze.Beans.Game.Bag.BDestroy, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = 237210527;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Destroy() {
        Argument = new Zeze.Beans.Game.Bag.BDestroy();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
