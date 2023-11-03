// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public interface BServiceReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BService copy();

    String getServiceName();
    String getState();
    String getPs();
}
