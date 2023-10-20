// auto-generated @formatter:off
package Zeze.Builtin.Auth;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAuth extends TableX<String, Zeze.Builtin.Auth.BAuthValue>
        implements TableReadOnly<String, Zeze.Builtin.Auth.BAuthValue, Zeze.Builtin.Auth.BAuthValueReadOnly> {
    public tAuth() {
        super(1601559288, "Zeze_Builtin_Auth_tAuth");
    }

    public tAuth(String suffix) {
        super(1601559288, "Zeze_Builtin_Auth_tAuth", suffix);
    }

    public static final int VAR_Auths = 1;

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
    public Zeze.Builtin.Auth.BAuthValue newValue() {
        return new Zeze.Builtin.Auth.BAuthValue();
    }

    @Override
    public Zeze.Builtin.Auth.BAuthValueReadOnly getReadOnly(String key) {
        return get(key);
    }
}
