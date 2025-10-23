// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BWalkReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BWalk copy();
    BWalk.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Net.Binary getExclusiveStartKey();
    int getProposeLimit();
    boolean isDesc();
    Zeze.Net.Binary getPrefix();
}
