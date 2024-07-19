// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class ZezeHistoryTable_m_a_g_i_c extends TableX<Zeze.Util.Id128, Zeze.Builtin.HistoryModule.BLogChanges>
        implements TableReadOnly<Zeze.Util.Id128, Zeze.Builtin.HistoryModule.BLogChanges, Zeze.Builtin.HistoryModule.BLogChangesReadOnly> {
    public ZezeHistoryTable_m_a_g_i_c() {
        super(584741651, "Zeze_Builtin_HistoryModule_ZezeHistoryTable_m_a_g_i_c");
    }

    public ZezeHistoryTable_m_a_g_i_c(String _s_) {
        super(584741651, "Zeze_Builtin_HistoryModule_ZezeHistoryTable_m_a_g_i_c", _s_);
    }

    @Override
    public Class<Zeze.Util.Id128> getKeyClass() {
        return Zeze.Util.Id128.class;
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
    public Zeze.Util.Id128 decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Util.Id128();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Util.Id128 _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Util.Id128 decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Util.Id128();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Util.Id128 _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChanges newValue() {
        return new Zeze.Builtin.HistoryModule.BLogChanges();
    }

    @Override
    public Zeze.Builtin.HistoryModule.BLogChangesReadOnly getReadOnly(Zeze.Util.Id128 _k_) {
        return get(_k_);
    }
}
