// auto-generated @formatter:off
package Zeze.Builtin.HttpSession;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is http sessionid from cookie
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tSession extends TableX<String, Zeze.Builtin.HttpSession.BSessionValue>
        implements TableReadOnly<String, Zeze.Builtin.HttpSession.BSessionValue, Zeze.Builtin.HttpSession.BSessionValueReadOnly> {
    public tSession() {
        super(223888315, "Zeze_Builtin_HttpSession_tSession");
    }

    public tSession(String suffix) {
        super(223888315, "Zeze_Builtin_HttpSession_tSession", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.HttpSession.BSessionValue> getValueClass() {
        return Zeze.Builtin.HttpSession.BSessionValue.class;
    }

    public static final int VAR_CreateTime = 1;
    public static final int VAR_ExpireTime = 2;
    public static final int VAR_Properties = 3;

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
    public Zeze.Builtin.HttpSession.BSessionValue newValue() {
        return new Zeze.Builtin.HttpSession.BSessionValue();
    }

    @Override
    public Zeze.Builtin.HttpSession.BSessionValueReadOnly getReadOnly(String key) {
        return get(key);
    }
}
