// auto-generated
package Zezex.Provider;

public class SetUserState extends Zeze.Net.Protocol1<Zezex.Provider.BSetUserState> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 54814;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SetUserState() {
        Argument = new Zezex.Provider.BSetUserState();
    }

}
