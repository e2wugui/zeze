// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class ModuleRedirectAllResult extends Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = 105409780;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47283400371444
    static { register(TypeId_, ModuleRedirectAllResult.class); }

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

    public ModuleRedirectAllResult() {
        Argument = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data();
    }

    public ModuleRedirectAllResult(Zeze.Builtin.ProviderDirect.BModuleRedirectAllResult.Data arg) {
        Argument = arg;
    }
}
