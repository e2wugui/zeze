// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BMQServersReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMQServers copy();

    Zeze.Builtin.MQ.Master.BMQInfoReadOnly getInfoReadOnly();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.MQ.Master.BMQServer, Zeze.Builtin.MQ.Master.BMQServerReadOnly> getServersReadOnly();
    long getSessionId();
}
