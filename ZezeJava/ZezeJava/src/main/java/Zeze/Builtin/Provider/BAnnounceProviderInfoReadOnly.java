// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// gs to link
public interface BAnnounceProviderInfoReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BAnnounceProviderInfo copy();
    BAnnounceProviderInfo.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getServiceNamePrefix();
    String getServiceIdentity();
    String getProviderDirectIp();
    int getProviderDirectPort();
    long getAppVersion();
    boolean isDisableChoice();
}
