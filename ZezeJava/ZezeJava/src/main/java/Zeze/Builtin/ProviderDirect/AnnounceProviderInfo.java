// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class AnnounceProviderInfo extends Zeze.Net.Rpc<Zeze.Builtin.ProviderDirect.BProviderInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = -1548813974; // 2746153322
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47286041114986

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
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public AnnounceProviderInfo(Zeze.Builtin.ProviderDirect.BProviderInfo arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
