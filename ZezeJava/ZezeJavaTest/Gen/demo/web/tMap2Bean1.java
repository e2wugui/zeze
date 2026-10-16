// auto-generated @formatter:off
package demo.web;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.web.BMap2Bean1ReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tMap2Bean1 extends TableX<Long, demo.web.BMap2Bean1>
        implements TableReadOnly<Long, demo.web.BMap2Bean1, demo.web.BMap2Bean1ReadOnly> {
    public tMap2Bean1() {
        super(-855147520, "demo_web_tMap2Bean1");
    }

    public tMap2Bean1(String _s_) {
        super(-855147520, "demo_web_tMap2Bean1", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.web.BMap2Bean1> getValueClass() {
        return demo.web.BMap2Bean1.class;
    }

    public static final int VAR_map2 = 1;

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
    public demo.web.BMap2Bean1 newValue() {
        return new demo.web.BMap2Bean1();
    }

    @Override
    public @Nullable BMap2Bean1ReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
