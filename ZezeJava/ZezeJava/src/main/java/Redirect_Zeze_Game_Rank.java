// auto-generated @formatter:off
public final class Redirect_Zeze_Game_Rank extends Zeze.Game.Rank {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected Zeze.Arch.RedirectAllFuture<Zeze.Game.Rank.RRankList> getRankAll(int arg0, Zeze.Builtin.Game.Rank.BConcurrentKey arg1) {
        var _c_ = new Zeze.Arch.RedirectAllContext<>(arg0, _params_ -> {
            var _r_ = new Zeze.Game.Rank.RRankList();
            if (_params_ != null) {
                var _b_ = _params_.Wrap();
                _r_.rankList.Decode(_b_);
            }
            return _r_;
        });
        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11015);
        _a_.setHashCodeConcurrentLevel(arg0);
        _a_.setMethodFullName("Zeze.Game.Rank:getRankAll");
        _a_.setServiceNamePrefix(_redirect_.ProviderApp.ServerServiceNamePrefix);
        _a_.setSessionId(_redirect_.ProviderApp.ProviderDirectService.AddManualContextWithTimeout(_c_));
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        arg1.Encode(_b_);
        _a_.setParams(new Zeze.Net.Binary(_b_));
        return _redirect_.RedirectAll(this, _p_, _c_);
    }

    @Override
    public Zeze.Arch.RedirectFuture<Long> updateRank(int arg0, Zeze.Builtin.Game.Rank.BConcurrentKey arg1, long arg2, long arg3, Zeze.Net.Binary arg4) {
        var _t_ = _redirect_.ChoiceHash(this, arg0);
        if (_t_ == null) { // local: loop-back
            return _redirect_.RunFuture(Zeze.Transaction.TransactionLevel.Serializable,
                () -> super.updateRank(arg0, arg1, arg2, arg3, arg4));
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11015);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash);
        _a_.setHashCode(arg0);
        _a_.setMethodFullName("Zeze.Game.Rank:updateRank");
        _a_.setServiceNamePrefix(_redirect_.ProviderApp.ServerServiceNamePrefix);
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        arg1.Encode(_b_);
        _b_.WriteLong(arg2);
        _b_.WriteLong(arg3);
        _b_.WriteBinary(arg4);
        _a_.setParams(new Zeze.Net.Binary(_b_));

        var _f_ = new Zeze.Arch.RedirectFuture<Long>();
        _p_.Send(_t_, _rpc_ -> {
            _f_.SetResult(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());
            return Zeze.Transaction.Procedure.Success;
        });
        return _f_;
    }

    public Redirect_Zeze_Game_Rank(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().Redirect;

        _app_.getZeze().Redirect.Handles.put("Zeze.Game.Rank:getRankAll", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                var _b_ = _params_.Wrap();
                var arg1 = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                arg1.Decode(_b_);
                return super.getRankAll(_hash_, arg1);
            }, _result_ -> {
                var _r_ = (Zeze.Game.Rank.RRankList)_result_;
                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
                _r_.rankList.Encode(_b_);
                return new Zeze.Net.Binary(_b_);
            }));
        _app_.getZeze().Redirect.Handles.put("Zeze.Game.Rank:updateRank", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                var arg1 = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                long arg2;
                long arg3;
                Zeze.Net.Binary arg4;
                var _b_ = _params_.Wrap();
                arg1.Decode(_b_);
                arg2 = _b_.ReadLong();
                arg3 = _b_.ReadLong();
                arg4 = _b_.ReadBinary();
                return super.updateRank(_hash_, arg1, arg2, arg3, arg4);
            }, _result_ -> Zeze.Net.Binary.Empty));
    }
}
