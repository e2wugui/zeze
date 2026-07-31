// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tSafeBatchSortedMap extends TableX<String, Zeze.Builtin.SafeBatch.BBatchSortedMap>
        implements TableReadOnly<String, Zeze.Builtin.SafeBatch.BBatchSortedMap, Zeze.Builtin.SafeBatch.BBatchSortedMapReadOnly> {
    public tSafeBatchSortedMap() {
        super(-2134918459, "Zeze_Builtin_SafeBatch_tSafeBatchSortedMap");
    }

    public tSafeBatchSortedMap(String _s_) {
        super(-2134918459, "Zeze_Builtin_SafeBatch_tSafeBatchSortedMap", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.SafeBatch.BBatchSortedMap> getValueClass() {
        return Zeze.Builtin.SafeBatch.BBatchSortedMap.class;
    }

    public static final int VAR_TableName = 1;
    public static final int VAR_RecordKey = 2;
    public static final int VAR_LastMapKey = 3;
    public static final int VAR_ProposeLimit = 4;
    public static final int VAR_JobClass = 5;
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
    public Zeze.Builtin.SafeBatch.BBatchSortedMap newValue() {
        return new Zeze.Builtin.SafeBatch.BBatchSortedMap();
    }

    @Override
    public Zeze.Builtin.SafeBatch.BBatchSortedMapReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
