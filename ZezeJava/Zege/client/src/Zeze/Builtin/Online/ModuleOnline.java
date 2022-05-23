package Zeze.Builtin.Online;

import Zeze.Util.TaskCompletionSource;

public class ModuleOnline extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessSReliableNotify(Zeze.Builtin.Online.SReliableNotify p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    public TaskCompletionSource<Zeze.Transaction.EmptyBean> login(String clientId) {
        var req = new Login();
        req.Argument.setClientId(clientId);
        return req.SendForWait(App.Connector.GetReadySocket());
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleOnline(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
