// auto-generated @formatter:off
package Zeze.Builtin.LoginQueueServer;

public class ReportLinkLoad extends Zeze.Net.Protocol<Zeze.Builtin.LoginQueueServer.BProviderLoad.Data> {
    public static final int ModuleId_ = 11042;
    public static final int ProtocolId_ = -1724960337; // 2570006959
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47427598889391
    static { register(TypeId_, ReportLinkLoad.class); }

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

    public ReportLinkLoad() {
        Argument = new Zeze.Builtin.LoginQueueServer.BProviderLoad.Data();
    }

    public ReportLinkLoad(Zeze.Builtin.LoginQueueServer.BProviderLoad.Data arg) {
        Argument = arg;
    }
}
