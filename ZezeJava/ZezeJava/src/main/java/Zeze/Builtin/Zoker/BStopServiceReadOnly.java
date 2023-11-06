// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public interface BStopServiceReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BStopService copy();

    String getServiceName();
    boolean isForce();
}
