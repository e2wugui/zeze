// auto-generated @formatter:off
package Zeze.Builtin.SafeBatch;

public interface BBatchSortedMapReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BBatchSortedMap copy();
    BBatchSortedMap.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getTableName();
    Zeze.Net.Binary getRecordKey();
    Zeze.Net.Binary getLastMapKey();
    int getProposeLimit();
    String getJobClass();
    Zeze.Net.Binary getOneByOneKey();
}
