// auto-generated @formatter:off
package Zeze.Builtin.Online;

public class SReliableNotify extends Zeze.Net.Protocol<Zeze.Builtin.Online.BReliableNotify> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = 240607704;
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
        Argument = new Zeze.Builtin.Online.BReliableNotify();
    }
}
