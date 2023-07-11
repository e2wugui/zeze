// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BObjectReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BObject copy();

    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
    Zeze.Builtin.World.BMoveReadOnly getMovingReadOnly();
    String getPlayerId();
    String getLinkName();
    long getLinkSid();
}
