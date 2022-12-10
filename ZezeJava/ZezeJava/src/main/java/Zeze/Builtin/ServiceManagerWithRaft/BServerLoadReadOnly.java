// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BServerLoadReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BServerLoad copy();

    public String getIp();
    public int getPort();
    public Zeze.Net.Binary getParam();
}
