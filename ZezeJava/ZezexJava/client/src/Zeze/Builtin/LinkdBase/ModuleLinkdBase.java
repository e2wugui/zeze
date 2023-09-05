package Zeze.Builtin.LinkdBase;

import Zeze.Transaction.Procedure;

public class ModuleLinkdBase extends AbstractModule {
    public void Start(ClientGame.App app) throws Exception {
    }

    @Override
    public void StartLast() throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    @Override
    protected long ProcessReportError(Zeze.Builtin.LinkdBase.ReportError p) {
        System.out.println("ReportError code=" + p.Argument.getCode() + " desc=" + p.Argument.getDesc());
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkdBase(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
