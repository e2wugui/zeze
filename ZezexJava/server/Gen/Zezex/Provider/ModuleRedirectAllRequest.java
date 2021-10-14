// auto-generated
package Zezex.Provider;

public class ModuleRedirectAllRequest extends Zeze.Net.Protocol1<Zezex.Provider.BModuleRedirectAllRequest> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 53858;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ModuleRedirectAllRequest() {
        Argument = new Zezex.Provider.BModuleRedirectAllRequest();
    }

}
