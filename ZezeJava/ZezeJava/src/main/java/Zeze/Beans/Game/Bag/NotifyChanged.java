// auto-generated @formatter:off
package Zeze.Beans.Game.Bag;

public class NotifyChanged extends Zeze.Net.Protocol1<Zeze.Beans.Game.Bag.BChanged> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = 649939829;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public NotifyChanged() {
        Argument = new Zeze.Beans.Game.Bag.BChanged();
    }
}
