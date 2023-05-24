// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

// 使用protocol而不是rpc，是为了可以按分组返回结果，当然现在定义支持一个结果里面包含多个分组结果
public class ModuleRedirectAllRequest extends Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = -773666772; // 3521300524
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47286816262188
    static { register(TypeId_, ModuleRedirectAllRequest.class); }

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

    public ModuleRedirectAllRequest() {
        Argument = new Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data();
    }

    public ModuleRedirectAllRequest(Zeze.Builtin.ProviderDirect.BModuleRedirectAllRequest.Data arg) {
        Argument = arg;
    }
}
