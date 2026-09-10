// auto-generated @formatter:off
package metagame.builtin.World;

// MoveMmo
public interface BMoveReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BMove copy();
    BMove.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Serialize.Vector3 getPosition();
    Zeze.Serialize.Vector3 getDirect();
    int getState();
    int getControl();
    long getTimestamp();
}
