// auto-generated @formatter:off
package metagame.builtin.World;

public interface BObjectReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BObject copy();

    Zeze.Transaction.DynamicBeanReadOnly getDataReadOnly();
    metagame.builtin.World.BMoveReadOnly getMovingReadOnly();
    String getPlayerId();
    String getLinkName();
    long getLinkSid();
    int getType();
    int getConfigId();
}
