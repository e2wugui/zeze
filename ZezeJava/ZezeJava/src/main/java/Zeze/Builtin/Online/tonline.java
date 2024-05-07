// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tonline extends TableX<String, Zeze.Builtin.Online.BOnlines>
        implements TableReadOnly<String, Zeze.Builtin.Online.BOnlines, Zeze.Builtin.Online.BOnlinesReadOnly> {
    public tonline() {
        super(-2094601796, "Zeze_Builtin_Online_tonline");
    }

    public tonline(String suffix) {
        super(-2094601796, "Zeze_Builtin_Online_tonline", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Online.BOnlines> getValueClass() {
        return Zeze.Builtin.Online.BOnlines.class;
    }

    public static final int VAR_Logins = 1;
    public static final int VAR_LastLoginVersion = 2;
    public static final int VAR_Account = 3;

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
    public Zeze.Builtin.Online.BOnlines newValue() {
        return new Zeze.Builtin.Online.BOnlines();
    }

    @Override
    public Zeze.Builtin.Online.BOnlinesReadOnly getReadOnly(String key) {
        return get(key);
    }
}
