// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tDepartmentTree extends Zeze.Transaction.TableX<Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey, Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode> {
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

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey DecodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _v_ = new Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode NewValue() {
        return new Zeze.Builtin.Collections.DepartmentTree.BDepartmentTreeNode();
    }
}
