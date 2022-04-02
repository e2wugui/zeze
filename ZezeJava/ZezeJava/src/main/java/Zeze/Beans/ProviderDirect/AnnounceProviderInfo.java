// auto-generated @formatter:off
package Zeze.Beans.ProviderDirect;

public class AnnounceProviderInfo extends Zeze.Net.Rpc<Zeze.Beans.ProviderDirect.BProviderInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = 61259632;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AnnounceProviderInfo() {
        Argument = new Zeze.Beans.ProviderDirect.BProviderInfo();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
