// auto-generated @formatter:off
package Zeze.Builtin.Collections.DAG;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Table: 有向图边表
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tEdge extends TableX<Zeze.Builtin.Collections.DAG.BDAGEdgeKey, Zeze.Builtin.Collections.DAG.BDAGEdge>
        implements TableReadOnly<Zeze.Builtin.Collections.DAG.BDAGEdgeKey, Zeze.Builtin.Collections.DAG.BDAGEdge, Zeze.Builtin.Collections.DAG.BDAGEdgeReadOnly> {
    public tEdge() {
        super(1544681320, "Zeze_Builtin_Collections_DAG_tEdge");
    }

    public tEdge(String _s_) {
        super(1544681320, "Zeze_Builtin_Collections_DAG_tEdge", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Collections.DAG.BDAGEdgeKey> getKeyClass() {
        return Zeze.Builtin.Collections.DAG.BDAGEdgeKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.DAG.BDAGEdge> getValueClass() {
        return Zeze.Builtin.Collections.DAG.BDAGEdge.class;
    }

    public static final int VAR_From = 1;
    public static final int VAR_To = 2;

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdgeKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Collections.DAG.BDAGEdgeKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.DAG.BDAGEdgeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdgeKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Collections.DAG.BDAGEdgeKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Collections.DAG.BDAGEdgeKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdge newValue() {
        return new Zeze.Builtin.Collections.DAG.BDAGEdge();
    }

    @Override
    public Zeze.Builtin.Collections.DAG.BDAGEdgeReadOnly getReadOnly(Zeze.Builtin.Collections.DAG.BDAGEdgeKey _k_) {
        return get(_k_);
    }
}
