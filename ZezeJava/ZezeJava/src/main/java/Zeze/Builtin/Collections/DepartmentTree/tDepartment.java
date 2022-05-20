// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tDepartment extends Zeze.Transaction.TableX<String, Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot> {
    public tDepartment() {
        super("Zeze_Builtin_Collections_DepartmentTree_tDepartment");
    }

    @Override
    public int getId() {
        return -1108948075;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Root = 1;
    public static final int VAR_Managers = 2;
    public static final int VAR_NextDepartmentId = 3;
    public static final int VAR_ChildDepartments = 4;

    @Override
    public String DecodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot NewValue() {
        return new Zeze.Builtin.Collections.DepartmentTree.BDepartmentRoot();
    }
}
