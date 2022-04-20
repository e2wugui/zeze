// auto-generated @formatter:off
package Zeze.Beans.Game.Bag;

public class NotifyBag extends Zeze.Net.Protocol1<Zeze.Beans.Game.Bag.BBag> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = 698814419;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public NotifyBag() {
        Argument = new Zeze.Beans.Game.Bag.BBag();
    }
}
