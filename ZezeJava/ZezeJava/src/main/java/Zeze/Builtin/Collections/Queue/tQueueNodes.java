// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tQueueNodes extends TableX<Zeze.Builtin.Collections.Queue.BQueueNodeKey, Zeze.Builtin.Collections.Queue.BQueueNode>
        implements TableReadOnly<Zeze.Builtin.Collections.Queue.BQueueNodeKey, Zeze.Builtin.Collections.Queue.BQueueNode, Zeze.Builtin.Collections.Queue.BQueueNodeReadOnly> {
    public tQueueNodes() {
        super(-117984600, "Zeze_Builtin_Collections_Queue_tQueueNodes");
    }

    public tQueueNodes(String _s_) {
        super(-117984600, "Zeze_Builtin_Collections_Queue_tQueueNodes", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Collections.Queue.BQueueNodeKey> getKeyClass() {
        return Zeze.Builtin.Collections.Queue.BQueueNodeKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.Queue.BQueueNode> getValueClass() {
        return Zeze.Builtin.Collections.Queue.BQueueNode.class;
    }

    public static final int VAR_NextNodeId = 1;
    public static final int VAR_Values = 2;
    public static final int VAR_NextNodeKey = 3;

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNode newValue() {
        return new Zeze.Builtin.Collections.Queue.BQueueNode();
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeReadOnly getReadOnly(Zeze.Builtin.Collections.Queue.BQueueNodeKey _k_) {
        return get(_k_);
    }
}
