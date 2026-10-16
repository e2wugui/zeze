// auto-generated @formatter:off
package demo.ModuleGTable;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.ModuleGTable.BValue2ReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tGTable2 extends TableX<Long, demo.ModuleGTable.BValue2>
        implements TableReadOnly<Long, demo.ModuleGTable.BValue2, demo.ModuleGTable.BValue2ReadOnly> {
    public tGTable2() {
        super(-515193034, "demo_ModuleGTable_tGTable2");
    }

    public tGTable2(String _s_) {
        super(-515193034, "demo_ModuleGTable_tGTable2", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.ModuleGTable.BValue2> getValueClass() {
        return demo.ModuleGTable.BValue2.class;
    }

    public static final int VAR_gTable = 1;

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
    public demo.ModuleGTable.BValue2 newValue() {
        return new demo.ModuleGTable.BValue2();
    }

    @Override
    public @Nullable BValue2ReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
