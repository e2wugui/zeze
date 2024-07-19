// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tDepartmentTree extends TableX<Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode>
        implements TableReadOnly<Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNodeReadOnly> {
    public tDepartmentTree() {
        super(-1578893665, "Zeze_Builtin_Collections_DepartmentTree_tDepartmentTree");
    }

    public tDepartmentTree(String _s_) {
        super(-1578893665, "Zeze_Builtin_Collections_DepartmentTree_tDepartmentTree", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey> getKeyClass() {
        return Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode> getValueClass() {
        return Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode.class;
    }

    public static final int VAR_ParentDepartment = 1;
    public static final int VAR_Childs = 2;
    public static final int VAR_Name = 3;
    public static final int VAR_Managers = 4;
    public static final int VAR_Data = 5;

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode newValue() {
        return new Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode();
    }

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNodeReadOnly getReadOnly(Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _k_) {
        return get(_k_);
    }
}
