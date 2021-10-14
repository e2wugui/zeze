// auto-generated
package Zezex.Provider;

public class SendConfirm extends Zeze.Net.Protocol1<Zezex.Provider.BSendConfirm> {
    public final static int ModuleId_ = 10001;
    public final static int ProtocolId_ = 50509;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SendConfirm() {
        Argument = new Zezex.Provider.BSendConfirm();
    }

}
