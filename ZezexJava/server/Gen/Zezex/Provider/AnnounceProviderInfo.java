// auto-generated
package Zezex.Provider;

public class AnnounceProviderInfo extends Zeze.Net.Protocol1<Zezex.Provider.BAnnounceProviderInfo> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 25503;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AnnounceProviderInfo() {
        Argument = new Zezex.Provider.BAnnounceProviderInfo();
    }

}
