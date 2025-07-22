// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

public class ReportProviderLoad extends Zeze.Net.Protocol<Zeze.Builtin.LoginQueueServer.BServerLoad.Data> {
    public static final int ModuleId_ = 11042;
    public static final int ProtocolId_ = 980883351;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47426009765783
    static { register(TypeId_, ReportProviderLoad.class); }

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

    public ReportProviderLoad() {
        Argument = new Zeze.Builtin.LoginQueueServer.BServerLoad.Data();
    }

    public ReportProviderLoad(Zeze.Builtin.LoginQueueServer.BServerLoad.Data arg) {
        Argument = arg;
    }
}
