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

    public tNode(String suffix) {
        super(-1059152625, "Zeze_Builtin_Collections_DAG_tNode", suffix);
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
        Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
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
    public Zeze.Builtin.Collections.DAG.BDAGNodeKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_ = new Zeze.Builtin.Collections.DAG.BDAGNodeKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.Collections.DAG.BDAGNodeKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNode newValue() {
        return new Zeze.Builtin.Collections.DAG.BDAGNode();
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGNodeReadOnly getReadOnly(Zeze.Builtin.Collections.DAG.BDAGNodeKey key) {
        return get(key);
    }
}
