// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BMQServersReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BMQServers copy();
    BMQServers.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Builtin.MQ.Master.BMQInfoReadOnly getInfoReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.MQ.Master.BMQServer, Zeze.Builtin.MQ.Master.BMQServerReadOnly> getServersReadOnly();
    long getSessionId();
}
