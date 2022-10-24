// auto-generated @formatter:off
package Zeze.Builtin.AutoKey;

public interface BAutoKeyReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BAutoKey copy();

    public long getNextId();
}
