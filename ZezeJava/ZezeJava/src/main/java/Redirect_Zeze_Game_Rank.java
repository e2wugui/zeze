// auto-generated @formatter:off

public class Redirect_Zeze_Game_Rank extends Zeze.Game.Rank {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    public Zeze.Arch.RedirectFuture<Long> removeRank(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint, long roleId) {
        var _f_ = new Zeze.Arch.RedirectFuture<Long>();
        try {
            var _t_ = _redirect_.choiceHash(this, hash, getConcurrentLevel(keyHint.getRankType()));
            if (_t_ == null) { // local: loop-back
                return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.Serializable,
                    () -> super.removeRank(hash, keyHint, roleId), "RedirectLoopBack_removeRank");
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
            var _m_ = 0L;
            if (keyHint == null)
                _m_ += 0x1L;
            _b_.WriteULong(_m_);
            if (keyHint != null) {
                keyHint.encode(_b_);
            }
            _b_.WriteLong(roleId);
            _a_.setParams(new Zeze.Net.Binary(_b_));

            if (!_p_.Send(_t_, _rpc_ -> {
                if (_rpc_.isTimeout()) {
                    _f_.setException(Zeze.Arch.RedirectException.timeoutInstance);
                    return Zeze.Transaction.Procedure.Success;
                }
                var _c_ = _rpc_.getResultCode();
                if (_c_ != Zeze.Transaction.Procedure.Success) {
                    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.REMOTE_EXECUTION, "resultCode=" + _c_));
                    return Zeze.Transaction.Procedure.Success;
                }
                _f_.setResult(_rpc_.Result.isNullParam() ? null : Zeze.Serialize.ByteBuffer.Wrap(_rpc_.Result.getParams()).ReadLong());
                return Zeze.Transaction.Procedure.Success;
            }, 30000)) {
                _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, "not found hash=" + hash));
            }
        } catch (Exception e) {
            _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.LOCAL_EXECUTION, e.getMessage(), e));
        }
        return _f_;
    }

    @Override
    public Zeze.Arch.RedirectFuture<Long> updateRank(int hash, Zeze.Builtin.Game.Rank.BConcurrentKey keyHint, long roleId, Zeze.Transaction.Bean value) {
        var _f_ = new Zeze.Arch.RedirectFuture<Long>();
        try {
            var _t_ = _redirect_.choiceHash(this, hash, getConcurrentLevel(keyHint.getRankType()));
            if (_t_ == null) { // local: loop-back
                return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.Serializable,
                    () -> super.updateRank(hash, keyHint, roleId, value), "RedirectLoopBack_updateRank");
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
            var _m_ = 0L;
            if (keyHint == null)
                _m_ += 0x1L;
            if (value == null)
                _m_ += 0x4L;
            _b_.WriteULong(_m_);
            if (keyHint != null) {
                keyHint.encode(_b_);
            }
            _b_.WriteLong(roleId);
            if (value != null) {
                _b_.WriteLong(value.typeId());
                value.encode(_b_);
            }
            _a_.setParams(new Zeze.Net.Binary(_b_));

            if (!_p_.Send(_t_, _rpc_ -> {
                if (_rpc_.isTimeout()) {
                    _f_.setException(Zeze.Arch.RedirectException.timeoutInstance);
                    return Zeze.Transaction.Procedure.Success;
                }
                var _c_ = _rpc_.getResultCode();
                if (_c_ != Zeze.Transaction.Procedure.Success) {
                    _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.REMOTE_EXECUTION, "resultCode=" + _c_));
                    return Zeze.Transaction.Procedure.Success;
                }
                _f_.setResult(_rpc_.Result.isNullParam() ? null : Zeze.Serialize.ByteBuffer.Wrap(_rpc_.Result.getParams()).ReadLong());
                return Zeze.Transaction.Procedure.Success;
            }, 30000)) {
                _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.SERVER_NOT_FOUND, "not found hash=" + hash));
            }
        } catch (Exception e) {
            _f_.setException(new Zeze.Arch.RedirectException(Zeze.Arch.RedirectException.LOCAL_EXECUTION, e.getMessage(), e));
        }
        return _f_;
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Game_Rank(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.Game.Rank:removeRank", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                Zeze.Builtin.Game.Rank.BConcurrentKey keyHint;
                long roleId;
                var _b_ = _params_.Wrap();
                var _m_ = _b_.ReadULong();
                if ((_m_ & 0x1L) == 0) {
                    keyHint = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                    keyHint.decode(_b_);
                } else
                    keyHint = null;
                if ((_m_ & 0x2L) == 0) {
                    roleId = _b_.ReadLong();
                } else
                    roleId = 0;
                return super.removeRank(_hash_, keyHint, roleId);
            }, null, 0));
        _app_.getZeze().redirect.handles.put("Zeze.Game.Rank:updateRank", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.Serializable, (_hash_, _params_) -> {
                Zeze.Builtin.Game.Rank.BConcurrentKey keyHint;
                long roleId;
                Zeze.Transaction.Bean value;
                var _b_ = _params_.Wrap();
                var _m_ = _b_.ReadULong();
                if ((_m_ & 0x1L) == 0) {
                    keyHint = new Zeze.Builtin.Game.Rank.BConcurrentKey();
                    keyHint.decode(_b_);
                } else
                    keyHint = null;
                if ((_m_ & 0x2L) == 0) {
                    roleId = _b_.ReadLong();
                } else
                    roleId = 0;
                if ((_m_ & 0x4L) == 0) {
                    value = beanFactory.createBeanFromSpecialTypeId(_b_.ReadLong());
                    value.decode(_b_);
                } else
                    value = null;
                return super.updateRank(_hash_, keyHint, roleId, value);
            }, null, 0));
    }
}
