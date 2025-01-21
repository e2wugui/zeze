// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// 角色离线时触发的定时器反向索引, 表名为"Zeze_Game_Online_tRoleOfflineTimers__{onlineSetName}", key是角色ID
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tRoleOfflineTimers extends TableX<Long, Zeze.Builtin.Timer.BOfflineTimers>
        implements TableReadOnly<Long, Zeze.Builtin.Timer.BOfflineTimers, Zeze.Builtin.Timer.BOfflineTimersReadOnly> {
    public tRoleOfflineTimers() {
        super(689434588, "Zeze_Builtin_Game_Online_tRoleOfflineTimers");
    }

    public tRoleOfflineTimers(String _s_) {
        super(689434588, "Zeze_Builtin_Game_Online_tRoleOfflineTimers", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BOfflineTimers> getValueClass() {
        return Zeze.Builtin.Timer.BOfflineTimers.class;
    }

    public static final int VAR_OfflineTimers = 1;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimers newValue() {
        return new Zeze.Builtin.Timer.BOfflineTimers();
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimersReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
