package Zege.Linkd;

import Zeze.Util.TaskCompletionSource;

public class ModuleLinkd extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessKeepAlive(Zege.Linkd.KeepAlive p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    public TaskCompletionSource<BAuthResult> auth(String account) {
        var a = new Auth();
        a.Argument.setAccount(account);
        return a.SendForWait(App.Connector.GetReadySocket());
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
