// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BServiceInfoReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BServiceInfo copy();

    public String getServiceName();
    public String getServiceIdentity();
    public String getPassiveIp();
    public int getPassivePort();
    public Zeze.Net.Binary getExtraInfo();
    public Zeze.Net.Binary getParam();
}
