// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class LinkBroken extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BLinkBroken.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -1642022578; // 2652944718
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281652939086
    static { register(TypeId_, LinkBroken.class); }

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

    public LinkBroken() {
        Argument = new Zeze.Builtin.Provider.BLinkBroken.Data();
    }

    public LinkBroken(Zeze.Builtin.Provider.BLinkBroken.Data arg) {
        Argument = arg;
    }
}
