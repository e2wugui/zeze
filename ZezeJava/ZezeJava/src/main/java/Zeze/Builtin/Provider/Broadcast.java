// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Broadcast extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BBroadcast> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -886924798; // 3408042498
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47282408036866

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Broadcast() {
        Argument = new Zeze.Builtin.Provider.BBroadcast();
    }
}
