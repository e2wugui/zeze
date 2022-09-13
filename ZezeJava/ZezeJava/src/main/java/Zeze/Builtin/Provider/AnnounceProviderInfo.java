// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class AnnounceProviderInfo extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BAnnounceProviderInfo> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 202613858;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47279202608226

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AnnounceProviderInfo() {
        Argument = new Zeze.Builtin.Provider.BAnnounceProviderInfo();
    }

    public AnnounceProviderInfo(Zeze.Builtin.Provider.BAnnounceProviderInfo arg) {
        Argument = arg;
    }
}
