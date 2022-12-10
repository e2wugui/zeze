// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BSubscribeInfoReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSubscribeInfo copy();

    public String getServiceName();
    public int getSubscribeType();
}
