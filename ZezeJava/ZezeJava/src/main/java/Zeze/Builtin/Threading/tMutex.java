// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tMutex extends TableX<String, Zeze.Builtin.Threading.BMutex>
        implements TableReadOnly<String, Zeze.Builtin.Threading.BMutex, Zeze.Builtin.Threading.BMutexReadOnly> {
    public tMutex() {
        super(-1314466074, "Zeze_Builtin_Threading_tMutex");
    }

    public tMutex(String suffix) {
        super(-1314466074, "Zeze_Builtin_Threading_tMutex", suffix);
    }

    public static final int VAR_Locked = 1;
    public static final int VAR_ServerId = 2;
    public static final int VAR_LockTime = 3;

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
    public Zeze.Builtin.Threading.BMutex newValue() {
        return new Zeze.Builtin.Threading.BMutex();
    }

    @Override
    public Zeze.Builtin.Threading.BMutexReadOnly getReadOnly(String key) {
        return get(key);
    }
}
