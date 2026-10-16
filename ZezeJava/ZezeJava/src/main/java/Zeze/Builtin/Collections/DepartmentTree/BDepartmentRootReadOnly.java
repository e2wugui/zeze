// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BDepartmentRootReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BDepartmentRoot copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getRoot();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getManagersReadOnly();
    long getNextDepartmentId();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, Long> getChildrenReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
}
