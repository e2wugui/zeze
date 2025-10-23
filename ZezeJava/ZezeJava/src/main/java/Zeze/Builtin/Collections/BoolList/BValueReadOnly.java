// auto-generated @formatter:off
package Zeze.Builtin.Collections.BoolList;

public interface BValueReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BValue copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getItem0();
    long getItem1();
    long getItem2();
    long getItem3();
    long getItem4();
    long getItem5();
    long getItem6();
    long getItem7();
}
