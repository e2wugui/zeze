// auto-generated @formatter:off
package Game.Buf;

// 所有的buf改变都通过这个协议发送，通过ChangeListener订阅。
public class SChanged extends Zeze.Net.Protocol<Game.Buf.BBufChanged> {
    public static final int ModuleId_ = 6;
    public static final int ProtocolId_ = -125583286; // 4169384010
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 29939187786
    static { register(TypeId_, SChanged.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public SChanged() {
        Argument = new Game.Buf.BBufChanged();
    }

    public SChanged(Game.Buf.BBufChanged arg) {
        Argument = arg;
    }
}
