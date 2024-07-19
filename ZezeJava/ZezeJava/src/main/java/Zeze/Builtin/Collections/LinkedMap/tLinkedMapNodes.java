// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tLinkedMapNodes extends TableX<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode>
        implements TableReadOnly<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeReadOnly> {
    public tLinkedMapNodes() {
        super(-1295098614, "Zeze_Builtin_Collections_LinkedMap_tLinkedMapNodes");
    }

    public tLinkedMapNodes(String _s_) {
        super(-1295098614, "Zeze_Builtin_Collections_LinkedMap_tLinkedMapNodes", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey> getKeyClass() {
        return Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode> getValueClass() {
        return Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode.class;
    }

    public static final int VAR_PrevNodeId = 1;
    public static final int VAR_NextNodeId = 2;
    public static final int VAR_Values = 3;

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode newValue() {
        return new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode();
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeReadOnly getReadOnly(Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey _k_) {
        return get(_k_);
    }
}
