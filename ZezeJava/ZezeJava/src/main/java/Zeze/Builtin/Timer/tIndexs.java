// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tIndexs extends TableX<String, Zeze.Builtin.Timer.BIndex>
        implements TableReadOnly<String, Zeze.Builtin.Timer.BIndex, Zeze.Builtin.Timer.BIndexReadOnly> {
    public tIndexs() {
        super(833718, "Zeze_Builtin_Timer_tIndexs");
    }

    public tIndexs(String suffix) {
        super(833718, "Zeze_Builtin_Timer_tIndexs", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BIndex> getValueClass() {
        return Zeze.Builtin.Timer.BIndex.class;
    }

    public static final int VAR_ServerId = 1;
    public static final int VAR_NodeId = 2;
    public static final int VAR_SerialId = 3;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public String decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        String _v_;
        _v_ = rs.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, String _v_) {
        st.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BIndex newValue() {
        return new Zeze.Builtin.Timer.BIndex();
    }

    @Override
    public Zeze.Builtin.Timer.BIndexReadOnly getReadOnly(String key) {
        return get(key);
    }
}
