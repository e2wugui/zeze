package Zeze.Arch;

public class ProviderLinkd extends AbstractProviderLinkd {
    @Override
    protected long ProcessAnnounceProviderInfo(Zeze.Beans.Provider.AnnounceProviderInfo p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessBindRequest(Zeze.Beans.Provider.Bind r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessBroadcast(Zeze.Beans.Provider.Broadcast p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessKick(Zeze.Beans.Provider.Kick p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessReportLoad(Zeze.Beans.Provider.ReportLoad p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSend(Zeze.Beans.Provider.Send p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSetUserState(Zeze.Beans.Provider.SetUserState p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSubscribeRequest(Zeze.Beans.Provider.Subscribe r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessUnBindRequest(Zeze.Beans.Provider.UnBind r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
