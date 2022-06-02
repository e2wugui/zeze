// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Send extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BSend> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -2067963426;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Send() {
        Argument = new Zeze.Builtin.Provider.BSend();
    }
}
