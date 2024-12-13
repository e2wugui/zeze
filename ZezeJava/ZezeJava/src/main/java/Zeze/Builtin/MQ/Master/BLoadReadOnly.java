// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public interface BLoadReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoad copy();

    double getLoad();
}
