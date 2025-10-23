// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BPushMessageReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BPushMessage copy();
    BPushMessage.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getTopic();
    int getPartitionIndex();
    long getSessionId();
    Zeze.Builtin.MQ.BMessageReadOnly getMessageReadOnly();
}
