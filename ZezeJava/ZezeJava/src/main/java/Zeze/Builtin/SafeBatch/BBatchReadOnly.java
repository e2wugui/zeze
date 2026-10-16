// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BBatchReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BBatch copy();
    BBatch.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getAppInstanceId();
    String getTableName();
    Zeze.Net.Binary getRecordKey();
    Zeze.Net.Binary getLastKey();
    int getProposeLimit();
    String getJobClass();
    int getWorker();
}
