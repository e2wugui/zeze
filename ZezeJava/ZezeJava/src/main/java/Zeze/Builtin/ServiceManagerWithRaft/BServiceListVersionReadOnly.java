// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BServiceListVersionReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BServiceListVersion copy();

    public String getServiceName();
    public long getSerialId();
}
