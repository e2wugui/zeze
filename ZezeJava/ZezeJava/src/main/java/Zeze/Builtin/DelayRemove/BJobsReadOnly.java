// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BJobsReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BJobs copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.DelayRemove.BJob, Zeze.Builtin.DelayRemove.BJobReadOnly> getJobsReadOnly();
}
