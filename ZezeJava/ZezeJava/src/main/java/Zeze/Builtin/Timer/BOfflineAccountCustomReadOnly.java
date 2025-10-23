// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// Offline Timer
public interface BOfflineAccountCustomReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BOfflineAccountCustom copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getTimerName();
    String getAccount();
    String getClientId();
    long getLoginVersion();
    String getHandleName();
    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
}
