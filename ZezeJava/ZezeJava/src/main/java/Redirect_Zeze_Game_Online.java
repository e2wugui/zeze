// auto-generated @formatter:off

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Redirect_Zeze_Game_Online extends Zeze.Game.Online {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected void redirectRemoveLocal(int serverId, long roleId, @Nullable String instanceName) {
        var _t_ = _redirect_.choiceServer(this, serverId, false);
        if (_t_ == null) { // local: loop-back
            _redirect_.runVoid(Zeze.Transaction.TransactionLevel.None,
                () -> super.redirectRemoveLocal(serverId, roleId, instanceName), "RedirectLoopBack_redirectRemoveLocal");
            return;
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11013);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(serverId);
        _a_.setKey(Long.hashCode(_t_.getSessionId()));
        _a_.setMethodFullName("Zeze.Game.Online:redirectRemoveLocal");
        _a_.setServiceNamePrefix(java.util.Objects.requireNonNull(_redirect_.providerApp.serverServiceNamePrefix));
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        var _m_ = 0L;
        if (instanceName == null)
            _m_ += 0x2L;
        _b_.WriteULong(_m_);
        _b_.WriteLong(roleId);
        if (instanceName != null) {
            _b_.WriteString(instanceName);
        }
        _a_.setParams(new Zeze.Net.Binary(_b_));

        _redirect_.sendVoid(_t_, _p_, "Zeze.Game.Online:redirectRemoveLocal");
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Game_Online(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(_app_.getZeze()).redirect);

        java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(_app_.getZeze()).redirect).handles.put("Zeze.Game.Online:redirectRemoveLocal", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.None, (_hash_, _params_) -> {
                long roleId;
                String instanceName;
                var _b_ = _params_.Wrap();
                var _m_ = _b_.ReadULong();
                if ((_m_ & 0x1L) == 0) {
                    roleId = _b_.ReadLong();
                } else
                    roleId = 0;
                if ((_m_ & 0x2L) == 0) {
                    instanceName = _b_.ReadString();
                } else
                    instanceName = null;
                super.redirectRemoveLocal(_hash_, roleId, instanceName);
                return null;
            }, null, 0));
    }
}
