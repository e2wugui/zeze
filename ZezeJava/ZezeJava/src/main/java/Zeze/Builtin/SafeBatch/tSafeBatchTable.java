// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

/*
                    public interface ITableJob {
                        void runJob(TableX<?, ?> table, ByteBuffer key, ByteBuffer value);
                        default Object decodeOneByOneKey(ByteBuffer key) {
                            return null;
                        }
                        default void encodeOneByOneKey(ByteBuffer buffer, Object key) {
                        }
                    }
*/
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tSafeBatchTable extends TableX<String, Zeze.Builtin.SafeBatch.BBatchTable>
        implements TableReadOnly<String, Zeze.Builtin.SafeBatch.BBatchTable, Zeze.Builtin.SafeBatch.BBatchTableReadOnly> {
    public tSafeBatchTable() {
        super(328514726, "Zeze_Builtin_SafeBatch_tSafeBatchTable");
    }

    public tSafeBatchTable(String _s_) {
        super(328514726, "Zeze_Builtin_SafeBatch_tSafeBatchTable", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.SafeBatch.BBatchTable> getValueClass() {
        return Zeze.Builtin.SafeBatch.BBatchTable.class;
    }

    public static final int VAR_TableName = 1;
    public static final int VAR_LastTableKey = 2;
    public static final int VAR_ProposeLimit = 3;
    public static final int VAR_JobClass = 4;
    public static final int VAR_TimerPeriod = 5;
    public static final int VAR_OneByOneKey = 6;

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
    public String decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        String _v_;
        _v_ = _s_.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, String _v_) {
        _s_.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatchTable newValue() {
        return new Zeze.Builtin.SafeBatch.BBatchTable();
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatchTableReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
