// auto-generated @formatter:off
public class Redirect_Zeze_Component_Timer extends Zeze.Component.Timer {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected void redirectCancel(int serverId, String timerId) {
        var _t_ = _redirect_.choiceServer(this, serverId, false);
        if (_t_ == null) { // local: loop-back
            _redirect_.runVoid(Zeze.Transaction.TransactionLevel.Serializable,
                () -> super.redirectCancel(serverId, timerId));
            return;
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11016);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(serverId);
        _a_.setKey(Long.hashCode(_t_.getSessionId()));
        _a_.setMethodFullName("Zeze.Component.Timer:redirectCancel");
        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        _b_.WriteString(timerId);
        _a_.setParams(new Zeze.Net.Binary(_b_));

        _p_.Send(_t_, null);
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Component_Timer(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.Component.Timer:redirectCancel", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                String timerId;
                var _b_ = _params_.Wrap();
                timerId = _b_.ReadString();
                super.redirectCancel(_hash_, timerId);
                return null;
            }, null, 0));
    }
}
