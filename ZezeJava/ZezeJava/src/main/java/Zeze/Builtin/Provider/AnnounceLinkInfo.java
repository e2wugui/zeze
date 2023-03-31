// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class AnnounceLinkInfo extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BAnnounceLinkInfo> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -1920287593; // 2374679703
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281374674071
    static { register(TypeId_, AnnounceLinkInfo.class); }

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

    public AnnounceLinkInfo() {
        Argument = new Zeze.Builtin.Provider.BAnnounceLinkInfo();
    }

    public AnnounceLinkInfo(Zeze.Builtin.Provider.BAnnounceLinkInfo arg) {
        Argument = arg;
    }
}
