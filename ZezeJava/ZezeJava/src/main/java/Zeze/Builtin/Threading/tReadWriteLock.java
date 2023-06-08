// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tReadWriteLock extends TableX<String, Zeze.Builtin.Threading.BReadWriteLock>
        implements TableReadOnly<String, Zeze.Builtin.Threading.BReadWriteLock, Zeze.Builtin.Threading.BReadWriteLockReadOnly> {
    public tReadWriteLock() {
        super(1528804574, "Zeze_Builtin_Threading_tReadWriteLock");
    }

    public tReadWriteLock(String suffix) {
        super(1528804574, "Zeze_Builtin_Threading_tReadWriteLock", suffix);
    }

    public static final int VAR_ReadingCount = 1;
    public static final int VAR_InWriting = 2;

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
    public Zeze.Builtin.Threading.BReadWriteLock newValue() {
        return new Zeze.Builtin.Threading.BReadWriteLock();
    }

    @Override
    public Zeze.Builtin.Threading.BReadWriteLockReadOnly getReadOnly(String key) {
        return get(key);
    }
}
