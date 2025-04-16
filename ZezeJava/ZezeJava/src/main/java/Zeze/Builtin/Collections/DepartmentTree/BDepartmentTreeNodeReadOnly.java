// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

public interface BDepartmentTreeNodeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDepartmentTreeNode copy();

    long getParentDepartment();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildrenReadOnly();
    String getName();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
}
