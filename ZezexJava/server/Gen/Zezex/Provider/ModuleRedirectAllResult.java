// auto-generated
package Zezex.Provider;

public class ModuleRedirectAllResult extends Zeze.Net.Protocol1<Zezex.Provider.BModuleRedirectAllResult> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 39817;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ModuleRedirectAllResult() {
        Argument = new Zezex.Provider.BModuleRedirectAllResult();
    }

}
