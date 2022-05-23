package Zege.Linkd;

public class ModuleLinkd extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessAuthRequest(Zege.Linkd.Auth r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessKeepAlive(Zege.Linkd.KeepAlive p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
