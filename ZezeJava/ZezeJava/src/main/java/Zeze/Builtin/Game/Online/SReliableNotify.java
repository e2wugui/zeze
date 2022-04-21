// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public class SReliableNotify extends Zeze.Net.Protocol1<Zeze.Builtin.Game.Online.BReliableNotify> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = 1941465447;
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
        Argument = new Zeze.Builtin.Game.Online.BReliableNotify();
    }
}
