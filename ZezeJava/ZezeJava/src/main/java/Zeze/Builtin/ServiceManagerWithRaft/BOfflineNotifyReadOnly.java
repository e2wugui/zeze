// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public interface BOfflineNotifyReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOfflineNotify copy();

    public int getServerId();
    public String getNotifyId();
    public long getNotifySerialId();
    public Zeze.Net.Binary getNotifyContext();
}
