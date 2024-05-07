// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key: LinkedMap的Name
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tLinkedMaps extends TableX<String, Zeze.Builtin.Collections.LinkedMap.BLinkedMap>
        implements TableReadOnly<String, Zeze.Builtin.Collections.LinkedMap.BLinkedMap, Zeze.Builtin.Collections.LinkedMap.BLinkedMapReadOnly> {
    public tLinkedMaps() {
        super(-72689413, "Zeze_Builtin_Collections_LinkedMap_tLinkedMaps");
    }

    public tLinkedMaps(String suffix) {
        super(-72689413, "Zeze_Builtin_Collections_LinkedMap_tLinkedMaps", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.LinkedMap.BLinkedMap> getValueClass() {
        return Zeze.Builtin.Collections.LinkedMap.BLinkedMap.class;
    }

    public static final int VAR_HeadNodeId = 1;
    public static final int VAR_TailNodeId = 2;
    public static final int VAR_Count = 3;
    public static final int VAR_LastNodeId = 4;

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
    public String decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        String _v_;
        _v_ = rs.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, String _v_) {
        st.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMap newValue() {
        return new Zeze.Builtin.Collections.LinkedMap.BLinkedMap();
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapReadOnly getReadOnly(String key) {
        return get(key);
    }
}
