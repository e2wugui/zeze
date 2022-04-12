package Zeze.Arch;

public class ProviderDirect extends AbstractProviderDirect {
    @Override
    protected long ProcessAnnounceProviderInfoRequest(Zeze.Beans.ProviderDirect.AnnounceProviderInfo r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessModuleRedirectRequest(Zeze.Beans.ProviderDirect.ModuleRedirect r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessModuleRedirectAllRequest(Zeze.Beans.ProviderDirect.ModuleRedirectAllRequest p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessModuleRedirectAllResult(Zeze.Beans.ProviderDirect.ModuleRedirectAllResult p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessTransmit(Zeze.Beans.ProviderDirect.Transmit p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
