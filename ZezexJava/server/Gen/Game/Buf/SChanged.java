// auto-generated
package Game.Buf;

public class SChanged extends Zeze.Net.Protocol1<Game.Buf.BBufChanged> {
    public final static int ModuleId_ = 6;
    public final static int ProtocolId_ = 14537;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SChanged() {
        Argument = new Game.Buf.BBufChanged();
    }

}
