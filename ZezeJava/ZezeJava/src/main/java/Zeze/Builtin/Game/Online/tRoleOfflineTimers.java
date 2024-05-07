// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tRoleOfflineTimers extends TableX<Long, Zeze.Builtin.Timer.BOfflineTimers>
        implements TableReadOnly<Long, Zeze.Builtin.Timer.BOfflineTimers, Zeze.Builtin.Timer.BOfflineTimersReadOnly> {
    public tRoleOfflineTimers() {
        super(689434588, "Zeze_Builtin_Game_Online_tRoleOfflineTimers");
    }

    public tRoleOfflineTimers(String suffix) {
        super(689434588, "Zeze_Builtin_Game_Online_tRoleOfflineTimers", suffix);
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
    public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        long _v_;
        _v_ = rs.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long _v_) {
        st.appendLong("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimers newValue() {
        return new Zeze.Builtin.Timer.BOfflineTimers();
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimersReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
