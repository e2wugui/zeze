// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tSemaphore extends TableX<String, Zeze.Builtin.Threading.BSemaphore>
        implements TableReadOnly<String, Zeze.Builtin.Threading.BSemaphore, Zeze.Builtin.Threading.BSemaphoreReadOnly> {
    public tSemaphore() {
        super(559329414, "Zeze_Builtin_Threading_tSemaphore");
    }

    public tSemaphore(String suffix) {
        super(559329414, "Zeze_Builtin_Threading_tSemaphore", suffix);
    }

    public static final int VAR_Permits = 1;
    public static final int VAR_InitialPermits = 2;

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
    public Zeze.Builtin.Threading.BSemaphore newValue() {
        return new Zeze.Builtin.Threading.BSemaphore();
    }

    @Override
    public Zeze.Builtin.Threading.BSemaphoreReadOnly getReadOnly(String key) {
        return get(key);
    }
}
