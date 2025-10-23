// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BRefusedReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BRefused copy();
    BRefused.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Dbh2.BBatch, Zeze.Builtin.Dbh2.BBatchReadOnly> getRefusedReadOnly();
}
