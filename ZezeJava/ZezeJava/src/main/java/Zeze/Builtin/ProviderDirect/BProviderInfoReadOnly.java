// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BProviderInfoReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BProviderInfo copy();

    public String getIp();
    public int getPort();
    public int getServerId();
}
