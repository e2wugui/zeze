// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.Bean1ReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tMemorySize extends TableX<Long, demo.Bean1>
        implements TableReadOnly<Long, demo.Bean1, demo.Bean1ReadOnly> {
    public tMemorySize() {
        super(1133765191, "demo_Module1_tMemorySize");
    }

    public tMemorySize(String _s_) {
        super(1133765191, "demo_Module1_tMemorySize", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Bean1> getValueClass() {
        return demo.Bean1.class;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_V1 = 1;
    public static final int VAR_V2 = 2;

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
    public demo.Bean1 newValue() {
        return new demo.Bean1();
    }

    @Override
    public @Nullable Bean1ReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
