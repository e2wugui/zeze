// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tDepartmentTree extends Zeze.Transaction.TableX<Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNodeReadOnly> {
    public tDepartmentTree() {
        super("Zeze_Builtin_Collections_DepartmentTree_tDepartmentTree");
    }

    @Override
    public int getId() {
        return -1578893665;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_ParentDepartment = 1;
    public static final int VAR_Childs = 2;
    public static final int VAR_Name = 3;
    public static final int VAR_Managers = 4;
    public static final int VAR_Data = 5;

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _v_ = new Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey();
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
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode newValue() {
        return new Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode();
    }

    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNodeReadOnly getReadOnly(Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey k) {
        return get(k);
    }
}
