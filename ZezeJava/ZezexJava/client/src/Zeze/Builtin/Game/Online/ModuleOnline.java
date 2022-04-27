package Zeze.Builtin.Game.Online;

public class ModuleOnline extends AbstractModule {
    public void Start(Client.App app) throws Throwable {
    }

    public void Stop(Client.App app) throws Throwable {
    }

    @Override
    protected long ProcessSReliableNotify(Zeze.Builtin.Game.Online.SReliableNotify p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleOnline(Client.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
