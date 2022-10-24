// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

public interface BDepartmentRootReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDepartmentRoot copy();

    public String getRoot();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly();
    public long getNextDepartmentId();
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildsReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();

}
