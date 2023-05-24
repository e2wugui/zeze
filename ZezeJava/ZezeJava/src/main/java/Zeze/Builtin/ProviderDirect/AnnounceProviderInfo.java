// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class AnnounceProviderInfo extends Zeze.Net.Rpc<Zeze.Builtin.ProviderDirect.BProviderInfo.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = -1548813974; // 2746153322
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47286041114986
    static { register(TypeId_, AnnounceProviderInfo.class); }

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

    public AnnounceProviderInfo() {
        Argument = new Zeze.Builtin.ProviderDirect.BProviderInfo.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public AnnounceProviderInfo(Zeze.Builtin.ProviderDirect.BProviderInfo.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
