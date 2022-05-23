package Zege.Linkd;

import Zeze.Arch.LinkdUserSession;
import Zeze.Builtin.LinkdBase.BReportError;

public class ModuleLinkd extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessAuthRequest(Zege.Linkd.Auth rpc) throws Throwable {
        var linkSession = (LinkdUserSession)rpc.getSender().getUserState();
        if (null == linkSession)
        {
            App.LinkdService.ReportError(rpc.getSender().getSessionId(),
                    BReportError.FromLink, BReportError.CodeNotAuthed,
                    "inner error.");
            return 0;
        }

        if (false == linkSession.TrySetAccount(rpc.Argument.getAccount()))
        {
            App.LinkdService.ReportError(rpc.getSender().getSessionId(),
                    BReportError.FromLink, BReportError.CodeNotAuthed,
                    "Account Can Not Change On Same Connection.");
            return 0;
        }

        var OriginSender = rpc.getSender();
        var OriginSessionId = rpc.SessionId;
        if (!App.LinkdApp.LinkdProvider.ChoiceProvider(rpc.getSender(),
                // 这个协议在Provider的User模块（不是linkd模块）处理。
                // 选择Provider的时候需要带上这个参数。只能配置。Ugly!
                BAuth.ProviderUserModuleId,
                (providerSocket) -> rpc.Send(providerSocket, (r) -> {
                    if (rpc.isTimeout())
                        rpc.setResultCode(Zeze.Transaction.Procedure.Timeout);

                    if (rpc.getResultCode() == 0)
                        linkSession.setAuthed();

                    // send result to OriginSender
                    rpc.setSender(OriginSender);
                    rpc.setSessionId(OriginSessionId);
                    rpc.SendResult();
                    return 0;
                }))) {
            App.LinkdService.ReportError(rpc.getSender().getSessionId(), BReportError.FromLink,
                    BReportError.CodeNoProvider, "no provider.");
        };
        return 0;
    }

    @Override
    protected long ProcessKeepAlive(Zege.Linkd.KeepAlive p) {
        var linkSession = (LinkdUserSession)p.getSender().getUserState();
        if (null == linkSession)
        {
            // handshake 完成之前不可能回收得到 keepalive，先这样处理吧。
            p.getSender().Close(null);
            return Zeze.Transaction.Procedure.LogicError;
        }
        linkSession.KeepAlive(App.LinkdService);
        p.getSender().Send(p); // send back;
        return Zeze.Transaction.Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
