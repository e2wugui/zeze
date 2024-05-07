// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tQueueLastTaskId extends TableX<String, Zeze.Builtin.RedoQueue.BTaskId>
        implements TableReadOnly<String, Zeze.Builtin.RedoQueue.BTaskId, Zeze.Builtin.RedoQueue.BTaskIdReadOnly> {
    public tQueueLastTaskId() {
        super(-1495051256, "Zeze_Builtin_RedoQueue_tQueueLastTaskId");
    }

    public tQueueLastTaskId(String suffix) {
        super(-1495051256, "Zeze_Builtin_RedoQueue_tQueueLastTaskId", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.RedoQueue.BTaskId> getValueClass() {
        return Zeze.Builtin.RedoQueue.BTaskId.class;
    }

    public static final int VAR_TaskId = 1;

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
    public Zeze.Builtin.RedoQueue.BTaskId newValue() {
        return new Zeze.Builtin.RedoQueue.BTaskId();
    }

    @Override
    public Zeze.Builtin.RedoQueue.BTaskIdReadOnly getReadOnly(String key) {
        return get(key);
    }
}
