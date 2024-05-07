// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tlocal extends TableX<String, Zeze.Builtin.Online.BLocals>
        implements TableReadOnly<String, Zeze.Builtin.Online.BLocals, Zeze.Builtin.Online.BLocalsReadOnly> {
    public tlocal() {
        super(-1858917951, "Zeze_Builtin_Online_tlocal");
    }

    public tlocal(String suffix) {
        super(-1858917951, "Zeze_Builtin_Online_tlocal", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Online.BLocals> getValueClass() {
        return Zeze.Builtin.Online.BLocals.class;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_Logins = 1;

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
    public Zeze.Builtin.Online.BLocals newValue() {
        return new Zeze.Builtin.Online.BLocals();
    }

    @Override
    public Zeze.Builtin.Online.BLocalsReadOnly getReadOnly(String key) {
        return get(key);
    }
}
