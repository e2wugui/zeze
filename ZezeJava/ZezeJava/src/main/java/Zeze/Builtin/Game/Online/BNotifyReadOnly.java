// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BNotifyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BNotify copy();

    Zeze.Net.Binary getFullEncodedProtocol();
}
