// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Send extends Zeze.Net.Rpc<Zeze.Builtin.Provider.BSend, Zeze.Builtin.Provider.BSendResult> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -2067963426; // 2227003870
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47281226998238

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
        Result = new Zeze.Builtin.Provider.BSendResult();
    }

    public Send(Zeze.Builtin.Provider.BSend arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Provider.BSendResult();
    }
}
