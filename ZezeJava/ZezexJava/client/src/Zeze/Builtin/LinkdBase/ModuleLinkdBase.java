package Zeze.Builtin.LinkdBase;

public class ModuleLinkdBase extends AbstractModule {
    public void Start(ClientGame.App app) throws Throwable {
    }

    public void Stop(ClientGame.App app) throws Throwable {
    }

    @Override
    protected long ProcessReportError(Zeze.Builtin.LinkdBase.ReportError p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkdBase(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
