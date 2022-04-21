// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class AnnounceProviderInfo extends Zeze.Net.Rpc<Zeze.Builtin.ProviderDirect.BProviderInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = -1548813974;
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
        Argument = new Zeze.Builtin.ProviderDirect.BProviderInfo();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
