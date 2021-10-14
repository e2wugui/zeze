// auto-generated
package Zezex.Provider;

public class Transmit extends Zeze.Net.Protocol1<Zezex.Provider.BTransmit> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 28188;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Transmit() {
        Argument = new Zezex.Provider.BTransmit();
    }

}
