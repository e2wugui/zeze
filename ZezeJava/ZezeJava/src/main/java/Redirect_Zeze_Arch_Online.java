// auto-generated @formatter:off
public class Redirect_Zeze_Arch_Online extends Zeze.Arch.Online {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected void redirectRemoveLocal(int serverId, String account) {
        var _t_ = _redirect_.choiceServer(this, serverId, false);
        if (_t_ == null) { // local: loop-back
            _redirect_.runVoid(Zeze.Transaction.TransactionLevel.None,
                () -> super.redirectRemoveLocal(serverId, account));
            return;
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11100);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(serverId);
        _a_.setKey(Long.hashCode(_t_.getSessionId()));
        _a_.setMethodFullName("Zeze.Arch.Online:redirectRemoveLocal");
        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        _b_.WriteString(account);
        _a_.setParams(new Zeze.Net.Binary(_b_));

        _p_.Send(_t_, null);
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Arch_Online(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.Arch.Online:redirectRemoveLocal", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.None, (_hash_, _params_) -> {
                String account;
                var _b_ = _params_.Wrap();
                account = _b_.ReadString();
                super.redirectRemoveLocal(_hash_, account);
                return null;
            }, null, 0));
    }
}
