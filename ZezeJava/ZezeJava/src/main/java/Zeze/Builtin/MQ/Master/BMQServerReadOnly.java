// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BMQServerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMQServer copy();

    String getHost();
    int getPort();
}
