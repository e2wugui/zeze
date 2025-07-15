package Zeze.Services;

public class LoginQueueServer extends AbstractLoginQueueServer {
    @Override
    protected long ProcessReportProviderLoadRequest(Zeze.Builtin.LoginQueueServer.ReportProviderLoad r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessReportLinkLoadRequest(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
