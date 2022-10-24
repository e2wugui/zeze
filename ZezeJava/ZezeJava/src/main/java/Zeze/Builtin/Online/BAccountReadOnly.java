// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BAccountReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BAccount copy();

    public long getLastLoginVersion();
}
