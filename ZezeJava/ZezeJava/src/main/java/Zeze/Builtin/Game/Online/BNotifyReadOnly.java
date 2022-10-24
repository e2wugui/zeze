// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BNotifyReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BNotify copy();

    public Zeze.Net.Binary getFullEncodedProtocol();
}
