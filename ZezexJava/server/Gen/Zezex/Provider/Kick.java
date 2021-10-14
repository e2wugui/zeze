// auto-generated
package Zezex.Provider;

public class Kick extends Zeze.Net.Protocol1<Zezex.Provider.BKick> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 20585;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Kick() {
        Argument = new Zezex.Provider.BKick();
    }

}
