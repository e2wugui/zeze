// auto-generated
package Game.Map;

public class SEnterWorldNow extends Zeze.Net.Protocol1<Game.Map.BEnterWorldNow> {
    public final static int ModuleId_ = 8;
    public final static int ProtocolId_ = 9983;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SEnterWorldNow() {
        Argument = new Game.Map.BEnterWorldNow();
    }

}
