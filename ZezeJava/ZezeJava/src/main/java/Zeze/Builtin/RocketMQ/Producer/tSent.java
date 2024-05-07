// auto-generated @formatter:off
package Zeze.Builtin.RocketMQ.Producer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tSent extends TableX<String, Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult>
        implements TableReadOnly<String, Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult, Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResultReadOnly> {
    public tSent() {
        super(1695098005, "Zeze_Builtin_RocketMQ_Producer_tSent");
    }

    public tSent(String suffix) {
        super(1695098005, "Zeze_Builtin_RocketMQ_Producer_tSent", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult> getValueClass() {
        return Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult.class;
    }

    public static final int VAR_Result = 1;
    public static final int VAR_Timestamp = 2;

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
    public Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult newValue() {
        return new Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult();
    }

    @Override
    public Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResultReadOnly getReadOnly(String key) {
        return get(key);
    }
}
