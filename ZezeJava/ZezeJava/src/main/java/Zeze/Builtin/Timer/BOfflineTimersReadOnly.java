// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 记录多个定时器(timerId)的反向索引
public interface BOfflineTimersReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BOfflineTimers copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getOfflineTimersReadOnly();
}
