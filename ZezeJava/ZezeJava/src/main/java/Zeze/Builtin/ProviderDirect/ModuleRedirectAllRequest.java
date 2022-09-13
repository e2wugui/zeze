// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class ModuleRedirectAllRequest extends Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = -773666772; // 3521300524
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47286816262188

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ModuleRedirectAllRequest() {
        Argument = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest();
    }

    public ModuleRedirectAllRequest(Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest arg) {
        Argument = arg;
    }
}
