// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BRegisterReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRegister copy();

    String getDbh2RaftAcceptorName();
    int getPort();
    int getBucketCount();
}
