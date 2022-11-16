// auto-generated @formatter:off
public final class Redirect_Zeze_Game_Online extends Zeze.Game.Online {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected Zeze.Arch.RedirectFuture<Long> redirectNotify(int serverId, long roleId) {
        var _t_ = _redirect_.choiceServer(this, serverId);
        if (_t_ == null) { // local: loop-back
            return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.Serializable,
                () -> super.redirectNotify(serverId, roleId));
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11013);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(serverId);
        _a_.setMethodFullName("Zeze.Game.Online:redirectNotify");
        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        _b_.WriteLong(roleId);
        _a_.setParams(new Zeze.Net.Binary(_b_));

        var _f_ = new Zeze.Arch.RedirectFuture<Long>();
        if (!_p_.Send(_t_, _rpc_ -> {
            _f_.setResult(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());
            return Zeze.Transaction.Procedure.Success;
        }, 5000)) {
            _f_.setResult(Zeze.Transaction.Procedure.ErrorSendFail);
        }
        return _f_;
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Game_Online(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.Game.Online:redirectNotify", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                long roleId;
                var _b_ = _params_.Wrap();
                roleId = _b_.ReadLong();
                return super.redirectNotify(_hash_, roleId);
            }, null));
    }
}
