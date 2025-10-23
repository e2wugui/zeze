// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 用于BTimer.CustomData,关联角色的offline timer上下文数据
public interface BOfflineRoleCustomReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BOfflineRoleCustom copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getTimerName();
    long getRoleId();
    long getLoginVersion();
    String getHandleName();
    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
    String getOnlineSetName();
}
