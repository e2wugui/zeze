// auto-generated @formatter:off
package dynamicred.Demo;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface OrderReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    Order copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getAmountsReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getQuantitiesReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getLinesReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getAmountGridReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getQuantityGridReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getDirectAmountReadOnly();
    dynamicred.Demo.AmountReadOnly getDirectAmount_dynamicred_Demo_AmountReadOnly();
    Zeze.Transaction.DynamicBeanReadOnly getDirectQuantityReadOnly();
    dynamicred.Demo.QuantityReadOnly getDirectQuantity_dynamicred_Demo_QuantityReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getQuantityMapReadOnly();
    Zeze.Transaction.Collections.PSortedMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getAmountSortedReadOnly();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly>> getQuantityGridSourceReadOnly();
}
