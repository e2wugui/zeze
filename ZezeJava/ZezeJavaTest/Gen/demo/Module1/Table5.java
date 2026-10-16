// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.Module2.BValueReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class Table5 extends TableX<Long, demo.Module2.BValue>
        implements TableReadOnly<Long, demo.Module2.BValue, demo.Module2.BValueReadOnly> {
    public Table5() {
        super(-1155541794, "demo_Module1_Table5");
    }

    public Table5(String _s_) {
        super(-1155541794, "demo_Module1_Table5", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Module2.BValue> getValueClass() {
        return demo.Module2.BValue.class;
    }

    public static final int VAR_s = 1;
    public static final int VAR_plist = 2;
    public static final int VAR_psortedmap = 3;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public demo.Module2.BValue newValue() {
        return new demo.Module2.BValue();
    }

    @Override
    public @Nullable BValueReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
