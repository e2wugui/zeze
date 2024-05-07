// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class ZezeHistoryTable_m_a_g_i_c extends TableX<Long, Zeze.Builtin.HistoryModule.BLogChanges>
        implements TableReadOnly<Long, Zeze.Builtin.HistoryModule.BLogChanges, Zeze.Builtin.HistoryModule.BLogChangesReadOnly> {
    public ZezeHistoryTable_m_a_g_i_c() {
        super(584741651, "Zeze_Builtin_HistoryModule_ZezeHistoryTable_m_a_g_i_c");
    }

    public ZezeHistoryTable_m_a_g_i_c(String suffix) {
        super(584741651, "Zeze_Builtin_HistoryModule_ZezeHistoryTable_m_a_g_i_c", suffix);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Zeze.Builtin.HistoryModule.BLogChanges> getValueClass() {
        return Zeze.Builtin.HistoryModule.BLogChanges.class;
    }

    public static final int VAR_GlobalSerialId = 1;
    public static final int VAR_ProtocolClassName = 2;
    public static final int VAR_ProtocolArgument = 3;
    public static final int VAR_Changes = 4;
    public static final int VAR_Timestamp = 5;

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
    public Zeze.Builtin.HistoryModule.BLogChanges newValue() {
        return new Zeze.Builtin.HistoryModule.BLogChanges();
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChangesReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
