package Zezex.Linkd;

public class ModuleLinkd extends AbstractModule {
    public void Start(Game.App app) throws Throwable {
    }

    public void Stop(Game.App app) throws Throwable {
    }

    @Override
    protected long ProcessKeepAlive(Zezex.Linkd.KeepAlive p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Game.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
