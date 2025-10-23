// auto-generated @formatter:off
package Zeze.Builtin.HttpSession;

public interface BSessionValueReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BSessionValue copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getCreateTime();
    long getExpireTime();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, String> getPropertiesReadOnly();
}
