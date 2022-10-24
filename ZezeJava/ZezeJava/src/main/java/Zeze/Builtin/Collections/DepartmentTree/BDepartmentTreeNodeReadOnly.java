// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

public interface BDepartmentTreeNodeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDepartmentTreeNode copy();

    public long getParentDepartment();
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildsReadOnly();
    public String getName();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();

}
