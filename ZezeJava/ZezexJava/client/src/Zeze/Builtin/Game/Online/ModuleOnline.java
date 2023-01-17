package Zeze.Builtin.Game.Online;

public class ModuleOnline extends AbstractModule {
    public void Start(ClientGame.App app) throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    @Override
    protected long ProcessSReliableNotify(Zeze.Builtin.Game.Online.SReliableNotify p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleOnline(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
