package ClientGame.Login;

import ClientZezex.Linkd.Auth;
import Zeze.IModule;

public class ModuleLogin extends AbstractModule {
    public void Start(ClientGame.App app) throws Exception {
    }

    @Override
    public void StartLast() throws Exception {
    }

    public void Stop(ClientGame.App app) throws Exception {
    }

    public void auth() throws InterruptedException {
        var r = new Auth();
        r.Argument.setAccount("TestHot");
        r.SendForWait(App.ClientService.GetSocket()).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("auth error=" + IModule.getErrorCode(r.getResultCode()));
    }

    public BRole getOrCreateRole(String roleName) {
        var r = new GetRoleList();
        r.SendForWait(App.ClientService.GetSocket(), 30_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("get role list error=" + IModule.getErrorCode(r.getResultCode()));

        for (var role : r.Result.getRoleList()) {
            if (role.getName().equals(roleName))
                return role;
        }
        return createRole(roleName);
    }

    public BRole createRole(String roleName) {
        var r = new CreateRole();
        r.Argument.setName(roleName);
        r.SendForWait(App.ClientService.GetSocket(), 30_000).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("create role error=" + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLogin(ClientGame.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
