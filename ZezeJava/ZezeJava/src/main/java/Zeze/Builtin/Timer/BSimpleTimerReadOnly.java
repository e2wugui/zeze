// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 固定周期触发的timer
public interface BSimpleTimerReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BSimpleTimer copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getDelay();
    long getPeriod();
    long getRemainTimes();
    long getHappenTimes();
    long getStartTime();
    long getEndTime();
    long getNextExpectedTime();
    long getExpectedTime();
    long getHappenTime();
    int getMissfirePolicy();
    String getOneByOneKey();
}
