// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 保存真正的用户自定义数据
public interface BOnlineCustomReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BOnlineCustom copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
}
