// auto-generated @formatter:off
package metagame.builtin.World;

public interface BObjectReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BObject copy();
    BObject.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
    metagame.builtin.World.BMoveReadOnly getMovingReadOnly();
    String getPlayerId();
    String getLinkName();
    long getLinkSid();
    int getType();
    int getConfigId();
}
