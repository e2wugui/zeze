package Zege.User;

import Zeze.Arch.ProviderUserSession;
import Zeze.Transaction.Procedure;

public class ModuleUser extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessAuthRequest(Zege.Linkd.Auth r) {
        // 【注意】此时还没有验证通过

        var session = ProviderUserSession.get(r);
        var user = _tUser.getOrAdd(session.getAccount());
        if (user.getCreateTime() == 0) {
            user.setCreateTime(System.currentTimeMillis());
        }
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleUser(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
