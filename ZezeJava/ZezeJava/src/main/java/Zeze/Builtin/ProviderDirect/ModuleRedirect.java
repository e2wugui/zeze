// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

// 转发只定义一个rpc，以后可能需要实现server之间的直连，不再通过转发
public class ModuleRedirect extends Zeze.Net.Rpc<Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data, Zeze.Builtin.ProviderDirect.BModuleRedirectResult.Data> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = 1107993902;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47284402955566
    static { register(TypeId_, ModuleRedirect.class); }

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

    public static final int RedirectTypeWithHash = 0;
    public static final int RedirectTypeToServer = 1;
    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeMethodFullNameNotFound = 1;
    public static final int ResultCodeHandleException = 2;
    public static final int ResultCodeHandleError = 3;
    public static final int ResultCodeHandleVersion = 4;
    public static final int ResultCodeLinkdTimeout = 10;
    public static final int ResultCodeLinkdNoProvider = 11;
    public static final int ResultCodeRequestTimeout = 12;

    public ModuleRedirect() {
        Argument = new Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data();
        Result = new Zeze.Builtin.ProviderDirect.BModuleRedirectResult.Data();
    }

    public ModuleRedirect(Zeze.Builtin.ProviderDirect.BModuleRedirectArgument.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.ProviderDirect.BModuleRedirectResult.Data();
    }
}
