// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tTestSchemas extends TableX<Long, demo.Module1.BTestSchemas>
        implements TableReadOnly<Long, demo.Module1.BTestSchemas, demo.Module1.BTestSchemasReadOnly> {
    public tTestSchemas() {
        super(1639309574, "demo_Module1_tTestSchemas");
    }

    public tTestSchemas(String _s_) {
        super(1639309574, "demo_Module1_tTestSchemas", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Module1.BTestSchemas> getValueClass() {
        return demo.Module1.BTestSchemas.class;
    }

    public static final int VAR_Var1 = 1;
    public static final int VAR_Var2 = 2;

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
    public demo.Module1.BTestSchemas newValue() {
        return new demo.Module1.BTestSchemas();
    }

    @Override
    public @Nullable BTestSchemasReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
