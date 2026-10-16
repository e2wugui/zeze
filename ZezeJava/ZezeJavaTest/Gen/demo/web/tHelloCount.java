// auto-generated @formatter:off
package demo.web;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tHelloCount extends TableX<Long, demo.web.BCount>
        implements TableReadOnly<Long, demo.web.BCount, demo.web.BCountReadOnly> {
    public tHelloCount() {
        super(-968344573, "demo_web_tHelloCount");
    }

    public tHelloCount(String _s_) {
        super(-968344573, "demo_web_tHelloCount", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.web.BCount> getValueClass() {
        return demo.web.BCount.class;
    }

    public static final int VAR_Count = 1;

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
    public demo.web.BCount newValue() {
        return new demo.web.BCount();
    }

    @Override
    public @Nullable BCountReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
