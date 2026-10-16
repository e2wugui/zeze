// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 使用cron表达式触发时间的timer
public interface BCronTimerReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BCronTimer copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getCronExpression();
    long getNextExpectedTime();
    long getExpectedTime();
    long getHappenTime();
    long getRemainTimes();
    long getEndTime();
    int getMissfirePolicy();
    String getOneByOneKey();
    long getHappenTimes();
}
