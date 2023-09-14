// auto-generated @formatter:off
public class Redirect_Zeze_World_World extends Zeze.World.World {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    public void redirectToServer(int serverId) {
        var _t_ = _redirect_.choiceServer(this, serverId, false);
        if (_t_ == null) { // local: loop-back
            _redirect_.runVoid(Zeze.Transaction.TransactionLevel.Serializable,
                () -> super.redirectToServer(serverId));
            return;
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11031);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(serverId);
        _a_.setKey(Long.hashCode(_t_.getSessionId()));
        _a_.setMethodFullName("Zeze.World.World:redirectToServer");
        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);

        _p_.Send(_t_, null);
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_World_World(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.World.World:redirectToServer", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                super.redirectToServer(_hash_);
                return null;
            }, null, 0));
    }
}
