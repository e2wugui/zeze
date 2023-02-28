// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// tables
public interface BOnlineReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnline copy();

    String getLinkName();
    long getLinkSid();
}
