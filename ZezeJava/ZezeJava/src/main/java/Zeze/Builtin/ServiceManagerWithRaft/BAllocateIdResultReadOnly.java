// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BAllocateIdResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BAllocateIdResult copy();

    public String getName();
    public long getStartId();
    public int getCount();
}
