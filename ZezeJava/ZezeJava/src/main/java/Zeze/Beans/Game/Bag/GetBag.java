// auto-generated @formatter:off
package Zeze.Beans.Game.Bag;

public class GetBag extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Beans.Game.Bag.BBag> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = 154545714;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public GetBag() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Zeze.Beans.Game.Bag.BBag();
    }
}
