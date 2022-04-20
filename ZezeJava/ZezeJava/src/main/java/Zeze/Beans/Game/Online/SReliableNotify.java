// auto-generated @formatter:off
package Zeze.Beans.Game.Online;

public class SReliableNotify extends Zeze.Net.Protocol1<Zeze.Beans.Game.Online.BReliableNotify> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = 1825544934;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SReliableNotify() {
        Argument = new Zeze.Beans.Game.Online.BReliableNotify();
    }
}
