// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// 账号离线时触发的定时器反向索引, key是账号名和客户端ID
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tAccountOfflineTimers extends TableX<Zeze.Builtin.Timer.BAccountClientId, Zeze.Builtin.Timer.BOfflineTimers>
        implements TableReadOnly<Zeze.Builtin.Timer.BAccountClientId, Zeze.Builtin.Timer.BOfflineTimers, Zeze.Builtin.Timer.BOfflineTimersReadOnly> {
    public tAccountOfflineTimers() {
        super(-865861330, "Zeze_Builtin_Timer_tAccountOfflineTimers");
    }

    public tAccountOfflineTimers(String _s_) {
        super(-865861330, "Zeze_Builtin_Timer_tAccountOfflineTimers", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Timer.BAccountClientId> getKeyClass() {
        return Zeze.Builtin.Timer.BAccountClientId.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BOfflineTimers> getValueClass() {
        return Zeze.Builtin.Timer.BOfflineTimers.class;
    }

    public static final int VAR_OfflineTimers = 1;

    @Override
    public Zeze.Builtin.Timer.BAccountClientId decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Timer.BAccountClientId();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Timer.BAccountClientId _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Timer.BAccountClientId decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Timer.BAccountClientId();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Timer.BAccountClientId _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimers newValue() {
        return new Zeze.Builtin.Timer.BOfflineTimers();
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimersReadOnly getReadOnly(Zeze.Builtin.Timer.BAccountClientId _k_) {
        return get(_k_);
    }
}
