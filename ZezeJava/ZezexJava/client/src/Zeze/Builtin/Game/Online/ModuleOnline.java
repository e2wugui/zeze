package Zeze.Builtin.Game.Online;

import Zeze.IModule;

public class ModuleOnline extends AbstractModule {
    public void Start(ClientGame.App app) throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    public void login(long roleId) {
        var r = new Zeze.Builtin.Game.Online.Login();
        r.Argument.setRoleId(roleId);
        r.SendForWait(App.ClientService.GetSocket(), 30_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("login error=" + IModule.getErrorCode(r.getResultCode()));
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
