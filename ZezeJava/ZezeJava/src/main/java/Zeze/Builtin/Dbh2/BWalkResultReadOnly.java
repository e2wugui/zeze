// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BWalkResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BWalkResult copy();
    BWalkResult.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Dbh2.BWalkKeyValue, Zeze.Builtin.Dbh2.BWalkKeyValueReadOnly> getKeyValuesReadOnly();
    boolean isBucketEnd();
    boolean isBucketRefuse();
}
