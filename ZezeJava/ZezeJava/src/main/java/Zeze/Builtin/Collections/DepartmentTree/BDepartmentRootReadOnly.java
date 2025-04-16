// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

public interface BDepartmentRootReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDepartmentRoot copy();

    String getRoot();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly();
    long getNextDepartmentId();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildrenReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
}
