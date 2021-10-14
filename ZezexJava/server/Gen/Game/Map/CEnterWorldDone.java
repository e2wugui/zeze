// auto-generated
package Game.Map;

public class CEnterWorldDone extends Zeze.Net.Protocol1<Game.Map.BEnterWorldDone> {
    public final static int ModuleId_ = 8;
    public final static int ProtocolId_ = 12744;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CEnterWorldDone() {
        Argument = new Game.Map.BEnterWorldDone();
    }

}
