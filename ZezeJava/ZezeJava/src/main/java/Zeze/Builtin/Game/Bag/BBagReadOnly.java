// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public interface BBagReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BBag copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getCapacity();
    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Builtin.Game.Bag.BItem, Zeze.Builtin.Game.Bag.BItemReadOnly> getItemsReadOnly();
}
