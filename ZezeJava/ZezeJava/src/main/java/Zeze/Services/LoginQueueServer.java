package Zeze.Services;

public class LoginQueueServer extends AbstractLoginQueueServer {
    @Override
    protected long ProcessReportProviderLoad(Zeze.Builtin.LoginQueueServer.ReportProviderLoad r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessReportLinkLoad(Zeze.Builtin.LoginQueueServer.ReportLinkLoad r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
