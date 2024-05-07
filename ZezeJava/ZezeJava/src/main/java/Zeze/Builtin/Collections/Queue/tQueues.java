// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key: Queue的Name
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tQueues extends TableX<String, Zeze.Builtin.Collections.Queue.BQueue>
        implements TableReadOnly<String, Zeze.Builtin.Collections.Queue.BQueue, Zeze.Builtin.Collections.Queue.BQueueReadOnly> {
    public tQueues() {
        super(1005923355, "Zeze_Builtin_Collections_Queue_tQueues");
    }

    public tQueues(String suffix) {
        super(1005923355, "Zeze_Builtin_Collections_Queue_tQueues", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.Queue.BQueue> getValueClass() {
        return Zeze.Builtin.Collections.Queue.BQueue.class;
    }

    public static final int VAR_HeadNodeId = 1;
    public static final int VAR_TailNodeId = 2;
    public static final int VAR_Count = 3;
    public static final int VAR_LastNodeId = 4;
    public static final int VAR_LoadSerialNo = 5;
    public static final int VAR_HeadNodeKey = 6;
    public static final int VAR_TailNodeKey = 7;

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
    public Zeze.Builtin.Collections.Queue.BQueue newValue() {
        return new Zeze.Builtin.Collections.Queue.BQueue();
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueReadOnly getReadOnly(String key) {
        return get(key);
    }
}
