// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class CloseSession extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSession.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = -989674523; // 3305292773
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47398269404133
    static { register(TypeId_, CloseSession.class); }

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

    public CloseSession() {
        Argument = new Zeze.Builtin.LogService.BSession.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public CloseSession(Zeze.Builtin.LogService.BSession.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
