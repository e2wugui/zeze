// auto-generated
package Zezex.Provider;

public class ModuleRedirect extends Zeze.Net.Rpc<Zezex.Provider.BModuleRedirectArgument, Zezex.Provider.BModuleRedirectResult> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 30314;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public final static int ResultCodeSuccess = 0;
    public final static int ResultCodeMethodFullNameNotFound = 1;
    public final static int ResultCodeHandleException = 2;
    public final static int ResultCodeHandleError = 3;
    public final static int ResultCodeLinkdTimeout = 10;
    public final static int ResultCodeLinkdNoProvider = 11;
    public final static int ResultCodeRequestTimeout = 12;

    public ModuleRedirect() {
        Argument = new Zezex.Provider.BModuleRedirectArgument();
        Result = new Zezex.Provider.BModuleRedirectResult();
    }

}
