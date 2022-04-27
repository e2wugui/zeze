package Zeze.Builtin.LinkdBase;

public class ModuleLinkdBase extends AbstractModule {
    public void Start(Game.App app) throws Throwable {
    }

    public void Stop(Game.App app) throws Throwable {
    }

    @Override
    protected long ProcessReportError(Zeze.Builtin.LinkdBase.ReportError p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkdBase(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
