// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tversion extends TableX<String, Zeze.Builtin.Online.BVersions>
        implements TableReadOnly<String, Zeze.Builtin.Online.BVersions, Zeze.Builtin.Online.BVersionsReadOnly> {
    public tversion() {
        super("Zeze_Builtin_Online_tversion");
    }

    @Override
    public int getId() {
        return -1179546366;
    }

    public static final int VAR_Logins = 1;
    public static final int VAR_LastLoginVersion = 2;

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
    public Zeze.Builtin.Online.BVersions newValue() {
        return new Zeze.Builtin.Online.BVersions();
    }

    @Override
    public Zeze.Builtin.Online.BVersionsReadOnly getReadOnly(String key) {
        return get(key);
    }
}
