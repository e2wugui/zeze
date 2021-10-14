// auto-generated
package Game.Login;

public class SReliableNotify extends Zeze.Net.Protocol1<Game.Login.BReliableNotify> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 40355;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SReliableNotify() {
        Argument = new Game.Login.BReliableNotify();
    }

}
