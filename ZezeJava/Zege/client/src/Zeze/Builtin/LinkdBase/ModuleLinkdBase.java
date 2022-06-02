package Zeze.Builtin.LinkdBase;

import Zeze.Transaction.Procedure;

public class ModuleLinkdBase extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessReportError(Zeze.Builtin.LinkdBase.ReportError p) {
        System.out.println("ReportError code=" + p.Argument.getCode() + " desc=" + p.Argument.getDesc());
        p.getSender().close();
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkdBase(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
