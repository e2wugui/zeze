// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tValueIdToNodeId extends TableX<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId>
        implements TableReadOnly<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeIdReadOnly> {
    public tValueIdToNodeId() {
        super(-1128401683, "Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId");
    }

    public tValueIdToNodeId(String suffix) {
        super(-1128401683, "Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId", suffix);
    }

    @Override
    public Class<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey> getKeyClass() {
        return Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId> getValueClass() {
        return Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId.class;
    }

    public static final int VAR_NodeId = 1;

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId newValue() {
        return new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId();
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeIdReadOnly getReadOnly(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey key) {
        return get(key);
    }
}
