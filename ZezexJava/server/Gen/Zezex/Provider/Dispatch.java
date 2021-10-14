// auto-generated
package Zezex.Provider;

public class Dispatch extends Zeze.Net.Protocol1<Zezex.Provider.BDispatch> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 45669;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Dispatch() {
        Argument = new Zezex.Provider.BDispatch();
    }

}
