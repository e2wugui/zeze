// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Send extends Zeze.Net.Rpc<Zeze.Arch.Beans.BSend, Zeze.Arch.Beans.BSendResult> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = -2067963426; // 2227003870
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47281226998238
    static { register(TypeId_, Send.class); }

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

    public Send() {
        Argument = new Zeze.Arch.Beans.BSend();
        Result = new Zeze.Arch.Beans.BSendResult();
    }

    public Send(Zeze.Arch.Beans.BSend arg) {
        Argument = arg;
        Result = new Zeze.Arch.Beans.BSendResult();
    }
}
