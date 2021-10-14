// auto-generated
package Zezex.Provider;

public class Broadcast extends Zeze.Net.Protocol1<Zezex.Provider.BBroadcast> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 52348;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Broadcast() {
        Argument = new Zezex.Provider.BBroadcast();
    }

}
