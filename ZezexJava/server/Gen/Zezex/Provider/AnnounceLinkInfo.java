// auto-generated
package Zezex.Provider;

public class AnnounceLinkInfo extends Zeze.Net.Protocol1<Zezex.Provider.BAnnounceLinkInfo> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 47599;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AnnounceLinkInfo() {
        Argument = new Zezex.Provider.BAnnounceLinkInfo();
    }

}
