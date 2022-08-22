package Zege.Linkd;

import Zege.User.VerifyChallengeResult;
import Zeze.Arch.LinkdUserSession;
import Zeze.Builtin.LinkdBase.BReportError;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;

public class ModuleLinkd extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessKeepAlive(Zege.Linkd.KeepAlive p) {
        var linkSession = (LinkdUserSession)p.getSender().getUserState();
        if (null == linkSession)
        {
            // handshake 完成之前不可能回收得到 keepalive，先这样处理吧。
            p.getSender().close();
            return Zeze.Transaction.Procedure.LogicError;
        }
        linkSession.KeepAlive(App.LinkdService);
        p.getSender().Send(p); // send back;
        return Zeze.Transaction.Procedure.Success;
    }

    private void verifyChallengeResult(Challenge c) throws Throwable {
        var v = new VerifyChallengeResult();
        v.Argument.setAccount(c.Result.getAccount());
        v.Argument.setRandomData(c.Argument.getRandomData());
        v.Argument.setSigned(c.Result.getSigned());

        if (!App.LinkdApp.LinkdProvider.ChoiceProvider(
                c.getSender(), v.getModuleId(),
                (providerSocket) -> v.Send(providerSocket, (r_) -> {
                    if (!v.isTimeout() && v.getResultCode() == 0) {
                        var linkSession = (LinkdUserSession)c.getSender().getUserState();
                        linkSession.setAccount(c.Result.getAccount());
                        linkSession.setAuthed();
                        new ChallengeOk().Send(c.getSender()); // skip result
                    } else
                        App.LinkdService.ReportError(
                                c.getSender().getSessionId(), BReportError.FromLink,
                                BReportError.CodeNotAuthed, "no provider.");
                    return 0;
                }))) {
            App.LinkdService.ReportError(
                    c.getSender().getSessionId(), BReportError.FromLink,
                    BReportError.CodeNoProvider, "no provider.");
        }
    }

    private void challenge(AsyncSocket sender) {
        var c = new Challenge();
        c.Send(sender, (c_) -> {
            if (c.isTimeout())
                return 0; // done; skip error;
            verifyChallengeResult(c);
            return 0;
        });
        // skip Send() result
    }

    @Override
    protected long ProcessChallengeMeRequest(Zege.Linkd.ChallengeMe r) {
        challenge(r.getSender());
        r.SendResult();
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
