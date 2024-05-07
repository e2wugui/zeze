// auto-generated @formatter:off
package Zeze.Builtin.Auth;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tRoleAuth extends TableX<String, Zeze.Builtin.Auth.BRoleAuth>
        implements TableReadOnly<String, Zeze.Builtin.Auth.BRoleAuth, Zeze.Builtin.Auth.BRoleAuthReadOnly> {
    public tRoleAuth() {
        super(85148051, "Zeze_Builtin_Auth_tRoleAuth");
    }

    public tRoleAuth(String suffix) {
        super(85148051, "Zeze_Builtin_Auth_tRoleAuth", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Auth.BRoleAuth> getValueClass() {
        return Zeze.Builtin.Auth.BRoleAuth.class;
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
    public Zeze.Builtin.Auth.BRoleAuth newValue() {
        return new Zeze.Builtin.Auth.BRoleAuth();
    }

    @Override
    public Zeze.Builtin.Auth.BRoleAuthReadOnly getReadOnly(String key) {
        return get(key);
    }
}
