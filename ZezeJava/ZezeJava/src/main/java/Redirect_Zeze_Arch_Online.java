// auto-generated @formatter:off
public final class Redirect_Zeze_Arch_Online extends Zeze.Arch.Online {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected void redirectNotify(int arg0, String arg1) {
        var _t_ = _redirect_.ChoiceServer(this, arg0);
        if (_t_ == null) { // local: loop-back
            _redirect_.RunVoid(Zeze.Transaction.TransactionLevel.Serializable,
                () -> super.redirectNotify(arg0, arg1));
            return;
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11100);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(arg0);
        _a_.setMethodFullName("Zeze.Arch.Online:redirectNotify");
        _a_.setServiceNamePrefix(_redirect_.ProviderApp.ServerServiceNamePrefix);
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        _b_.WriteString(arg1);
        _a_.setParams(new Zeze.Net.Binary(_b_));

        _p_.Send(_t_, null);
    }

    public Redirect_Zeze_Arch_Online(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().Redirect;

        _app_.getZeze().Redirect.Handles.put("Zeze.Arch.Online:redirectNotify", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                String arg1;
                var _b_ = _params_.Wrap();
                arg1 = _b_.ReadString();
                super.redirectNotify(_hash_, arg1);
                return null;
            }, _result_ -> Zeze.Net.Binary.Empty));
    }
}
