package Zege.Linkd;

import java.util.concurrent.Future;
import Zeze.Net.Binary;
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

    private String account;
    private TaskCompletionSource<Boolean> authFuture;
    public void setAccount(String account) {
        this.account = account;
        authFuture = new TaskCompletionSource<>();
    }

    public void waitAuthed() {
        authFuture.await();
    }

    @Override
    protected long ProcessChallengeRequest(Zege.Linkd.Challenge r) {
        r.Result.setAccount(account);
        // r.Argument.getRandomData(); // todo sign
        var signed = Binary.Empty;
        r.Result.setSigned(signed);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessChallengeOkRequest(Zege.Linkd.ChallengeOk r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
