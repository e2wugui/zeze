// auto-generated @formatter:off
public class Redirect_Zeze_Game_Rank extends Zeze.Game.Rank {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected Zeze.Arch.RedirectAllFuture<Zeze.Game.Rank.RRankList> getRankAll(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint) {
        var _c_ = new Zeze.Arch.RedirectAllContext<>(hash, _params_ -> {
            var _r_ = new Zeze.Game.Rank.RRankList();
            if (_params_ != null) {
                var _b_ = _params_.Wrap();
                _r_.rankList.decode(_b_);
            }
            return _r_;
        });
        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11015);
        _a_.setHashCodeConcurrentLevel(hash);
        _a_.setMethodFullName("Zeze.Game.Rank:getRankAll");
        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
        _a_.setSessionId(_redirect_.providerApp.providerDirectService.addManualContextWithTimeout(_c_, 10000));
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        keyHint.encode(_b_);
        _a_.setParams(new Zeze.Net.Binary(_b_));
        return _redirect_.redirectAll(this, _p_, _c_);
    }

    @Override
    public Zeze.Arch.RedirectFuture<Long> removeRank(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint, long roleId) {
        var _f_ = new Zeze.Arch.RedirectFuture<Long>();
        try {
            var _t_ = _redirect_.choiceHash(this, hash, getConcurrentLevel(keyHint.getRankType()));
            if (_t_ == null) { // local: loop-back
                return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.Serializable,
                    () -> super.removeRank(hash, keyHint, roleId));
            }

            var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
            var _a_ = _p_.Argument;
            _a_.setModuleId(11015);
            _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash);
            _a_.setHashCode(hash);
            _a_.setKey(Long.hashCode(_t_.getSessionId()));
            _a_.setMethodFullName("Zeze.Game.Rank:removeRank");
            _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
            var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.encode(_b_);
            _b_.WriteLong(roleId);
            _a_.setParams(new Zeze.Net.Binary(_b_));

            if (!_p_.Send(_t_, _rpc_ -> {
                if (_rpc_.isTimeout()) {
                    _f_.setException(Zeze.Arch.RedirectException.timeoutInstance);
                    return Zeze.Transaction.Procedure.Success;
                }
                _f_.setResult(_rpc_.getResultCode());
                return Zeze.Transaction.Procedure.Success;
            }, 5000)) {
                _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, "not found hash=" + hash));
            }
        } catch (Exception e) {
            _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, e.getMessage(), e));
        }
        return _f_;
    }

    @Override
    public Zeze.Arch.RedirectFuture<Long> updateRank(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx) {
        var _f_ = new Zeze.Arch.RedirectFuture<Long>();
        try {
            var _t_ = _redirect_.choiceHash(this, hash, getConcurrentLevel(keyHint.getRankType()));
            if (_t_ == null) { // local: loop-back
                return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.Serializable,
                    () -> super.updateRank(hash, keyHint, roleId, value, valueEx));
            }

            var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
            var _a_ = _p_.Argument;
            _a_.setModuleId(11015);
            _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash);
            _a_.setHashCode(hash);
            _a_.setKey(Long.hashCode(_t_.getSessionId()));
            _a_.setMethodFullName("Zeze.Game.Rank:updateRank");
            _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
            var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
            keyHint.encode(_b_);
            _b_.WriteLong(roleId);
            _b_.WriteLong(value);
            _b_.WriteBinary(valueEx);
            _a_.setParams(new Zeze.Net.Binary(_b_));

            if (!_p_.Send(_t_, _rpc_ -> {
                if (_rpc_.isTimeout()) {
                    _f_.setException(Zeze.Arch.RedirectException.timeoutInstance);
                    return Zeze.Transaction.Procedure.Success;
                }
                _f_.setResult(_rpc_.getResultCode());
                return Zeze.Transaction.Procedure.Success;
            }, 5000)) {
                _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, "not found hash=" + hash));
            }
        } catch (Exception e) {
            _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, e.getMessage(), e));
        }
        return _f_;
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Game_Rank(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.Game.Rank:getRankAll", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                var _b_ = _params_.Wrap();
                var keyHint = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                keyHint.decode(_b_);
                return super.getRankAll(_hash_, keyHint);
            }, _result_ -> {
                if (_result_ == null)
                    return Zeze.Net.Binary.Empty;
                var _r_ = (Zeze.Game.Rank.RRankList)_result_;
                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
                _r_.rankList.encode(_b_);
                return new Zeze.Net.Binary(_b_);
            }, 0));
        _app_.getZeze().redirect.handles.put("Zeze.Game.Rank:removeRank", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                var keyHint = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                long roleId;
                var _b_ = _params_.Wrap();
                keyHint.decode(_b_);
                roleId = _b_.ReadLong();
                return super.removeRank(_hash_, keyHint, roleId);
            }, null, 0));
        _app_.getZeze().redirect.handles.put("Zeze.Game.Rank:updateRank", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                var keyHint = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                long roleId;
                long value;
                Zeze.Net.Binary valueEx;
                var _b_ = _params_.Wrap();
                keyHint.decode(_b_);
                roleId = _b_.ReadLong();
                value = _b_.ReadLong();
                valueEx = _b_.ReadBinary();
                return super.updateRank(_hash_, keyHint, roleId, value, valueEx);
            }, null, 0));
    }
}
