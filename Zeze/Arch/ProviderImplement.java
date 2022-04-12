package Zeze.Arch;

public class ProviderImplement extends AbstractProviderImplement {
    @Override
    protected long ProcessAnnounceLinkInfo(Zeze.Beans.Provider.AnnounceLinkInfo p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessDispatch(Zeze.Beans.Provider.Dispatch p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessLinkBroken(Zeze.Beans.Provider.LinkBroken p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSendConfirm(Zeze.Beans.Provider.SendConfirm p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
