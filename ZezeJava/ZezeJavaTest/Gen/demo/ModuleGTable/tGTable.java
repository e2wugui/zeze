// auto-generated @formatter:off
package demo.ModuleGTable;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.ModuleGTable.BValueReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tGTable extends TableX<Long, demo.ModuleGTable.BValue>
        implements TableReadOnly<Long, demo.ModuleGTable.BValue, demo.ModuleGTable.BValueReadOnly> {
    public tGTable() {
        super(1854663379, "demo_ModuleGTable_tGTable");
    }

    public tGTable(String _s_) {
        super(1854663379, "demo_ModuleGTable_tGTable", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.ModuleGTable.BValue> getValueClass() {
        return demo.ModuleGTable.BValue.class;
    }

    public static final int VAR_gTable = 1;
    public static final int VAR_gTable2 = 2;
    public static final int VAR_gTable3 = 3;
    public static final int VAR_gTable4 = 4;

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
    public demo.ModuleGTable.BValue newValue() {
        return new demo.ModuleGTable.BValue();
    }

    @Override
    public @Nullable BValueReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
