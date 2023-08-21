package ClientGame.Login;

import ClientZezex.Linkd.Auth;

public class ModuleLogin extends AbstractModule {
    public void Start(ClientGame.App app) throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    public void auth() throws InterruptedException {
        var r = new Auth();
        r.Argument.setAccount("TestHot");
        r.SendForWait(App.ClientService.GetSocket()).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("auth error");
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
