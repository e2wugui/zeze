// auto-generated
package Zezex.Provider;

public class Send extends Zeze.Net.Protocol1<Zezex.Provider.BSend> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 30969;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Send() {
        Argument = new Zezex.Provider.BSend();
    }

}
