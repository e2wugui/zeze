// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// 群部门树根。普通用户也可以创建部门。暂不开放这个给个人。
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tDepartment extends TableX<String, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot>
        implements TableReadOnly<String, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly> {
    public tDepartment() {
        super(-1108948075, "Zeze_Builtin_Collections_DepartmentTree_tDepartment");
    }

    public tDepartment(String suffix) {
        super(-1108948075, "Zeze_Builtin_Collections_DepartmentTree_tDepartment", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot> getValueClass() {
        return Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot.class;
    }

    public static final int VAR_Root = 1;
    public static final int VAR_Managers = 2;
    public static final int VAR_NextDepartmentId = 3;
    public static final int VAR_Childs = 4;
    public static final int VAR_Data = 5;

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
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot newValue() {
        return new Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot();
    }

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentRootReadOnly getReadOnly(String key) {
        return get(key);
    }
}
