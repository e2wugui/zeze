// auto-generated @formatter:off
package Zeze.Builtin.RocketMQ.Producer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tSent extends TableX<String, Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult>
        implements TableReadOnly<String, Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult, Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResultReadOnly> {
    public tSent() {
        super("Zeze_Builtin_RocketMQ_Producer_tSent");
    }

    @Override
    public int getId() {
        return 1695098005;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
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
    public Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult newValue() {
        return new Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult();
    }

    @Override
    public Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResultReadOnly getReadOnly(String key) {
        return get(key);
    }
}