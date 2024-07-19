// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Table: 有向图节点表
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tNode extends TableX<Zeze.Builtin.Collections.DAG.BDAGNodeKey, Zeze.Builtin.Collections.DAG.BDAGNode>
        implements TableReadOnly<Zeze.Builtin.Collections.DAG.BDAGNodeKey, Zeze.Builtin.Collections.DAG.BDAGNode, Zeze.Builtin.Collections.DAG.BDAGNodeReadOnly> {
    public tNode() {
        super(-1059152625, "Zeze_Builtin_Collections_DAG_tNode");
    }

    public tNode(String _s_) {
        super(-1059152625, "Zeze_Builtin_Collections_DAG_tNode", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Collections.DAG.BDAGNodeKey> getKeyClass() {
        return Zeze.Builtin.Collections.DAG.BDAGNodeKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.DAG.BDAGNode> getValueClass() {
        return Zeze.Builtin.Collections.DAG.BDAGNode.class;
    }

    public static final int VAR_Value = 1;

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNode newValue() {
        return new Zeze.Builtin.Collections.DAG.BDAGNode();
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeReadOnly getReadOnly(Zeze.Builtin.Collections.DAG.BDAGNodeKey _k_) {
        return get(_k_);
    }
}
