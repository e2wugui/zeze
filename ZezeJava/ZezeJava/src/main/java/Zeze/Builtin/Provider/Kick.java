// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Kick extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BKick> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -73074142; // 4221893154
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47283221887522

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

    public Kick() {
        Argument = new Zeze.Builtin.Provider.BKick();
    }

    public Kick(Zeze.Builtin.Provider.BKick arg) {
        Argument = arg;
    }
}
