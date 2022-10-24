// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// gs to link
public interface BAnnounceProviderInfoReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BAnnounceProviderInfo copy();

    public String getServiceNamePrefix();
    public String getServiceIndentity();
    public String getProviderDirectIp();
    public int getProviderDirectPort();
}
