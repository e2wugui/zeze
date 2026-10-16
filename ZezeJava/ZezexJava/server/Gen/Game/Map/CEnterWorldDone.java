// auto-generated @formatter:off
package Game.Map;

// client load resource done
public class CEnterWorldDone extends Zeze.Net.Protocol<Game.Map.BEnterWorldDone> {
    public static final int ModuleId_ = 8;
    public static final int ProtocolId_ = 1345610236;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 35705348604
    static { register(TypeId_, CEnterWorldDone.class); }

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

    public CEnterWorldDone() {
        Argument = new Game.Map.BEnterWorldDone();
    }

    public CEnterWorldDone(Game.Map.BEnterWorldDone arg) {
        Argument = arg;
    }
}
