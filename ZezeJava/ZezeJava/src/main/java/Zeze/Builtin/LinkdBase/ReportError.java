// auto-generated @formatter:off
package Zeze.Builtin.LinkdBase;

public class ReportError extends Zeze.Net.Protocol<Zeze.Builtin.LinkdBase.BReportError.Data> {
    public static final int ModuleId_ = 11011;
    public static final int ProtocolId_ = 918874807;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47292803771063
    static { register(TypeId_, ReportError.class); }

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

    public ReportError() {
        Argument = new Zeze.Builtin.LinkdBase.BReportError.Data();
    }

    public ReportError(Zeze.Builtin.LinkdBase.BReportError.Data arg) {
        Argument = arg;
    }
}
