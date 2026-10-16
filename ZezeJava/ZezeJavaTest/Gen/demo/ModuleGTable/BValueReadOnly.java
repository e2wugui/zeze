// auto-generated @formatter:off
package demo.ModuleGTable;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BValueReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BValue copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap1<Integer, Integer>, Zeze.Transaction.GTable.BeanMap1ReadOnly<Integer, Integer>> getGTableReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap1<String, Integer>, Zeze.Transaction.GTable.BeanMap1ReadOnly<String, Integer>> getGTable2ReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getGTable3ReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<String, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getGTable4ReadOnly();
}
