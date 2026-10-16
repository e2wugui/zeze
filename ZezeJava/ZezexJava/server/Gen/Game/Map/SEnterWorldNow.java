// auto-generated @formatter:off
package Game.Map;

public class SEnterWorldNow extends Zeze.Net.Protocol<Game.Map.BEnterWorldNow> {
    public static final int ModuleId_ = 8;
    public static final int ProtocolId_ = 1699300278;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 36059038646
    static { register(TypeId_, SEnterWorldNow.class); }

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

    public SEnterWorldNow() {
        Argument = new Game.Map.BEnterWorldNow();
    }

    public SEnterWorldNow(Game.Map.BEnterWorldNow arg) {
        Argument = arg;
    }
}
