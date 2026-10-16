// auto-generated @formatter:off
package demo.Module1;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BFoodReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BFood copy();
    BFood.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.DynamicBeanReadOnly getSubclassReadOnly();
    demo.Bean1ReadOnly getSubclass_demo_Bean1ReadOnly();
    demo.Module1.BSimpleReadOnly getSubclass_demo_Module1_BSimpleReadOnly();
}
