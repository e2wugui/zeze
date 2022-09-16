// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class SetUserState extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BSetUserState> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -1725914489; // 2569052807
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281569047175

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SetUserState() {
        Argument = new Zeze.Builtin.Provider.BSetUserState();
    }

    public SetUserState(Zeze.Builtin.Provider.BSetUserState arg) {
        Argument = arg;
    }
}
