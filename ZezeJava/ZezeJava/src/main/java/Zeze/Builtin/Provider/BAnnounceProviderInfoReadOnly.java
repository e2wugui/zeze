// auto-generated @formatter:off
package Zeze.Builtin.Provider;

// gs to link
public interface BAnnounceProviderInfoReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAnnounceProviderInfo copy();

    String getServiceNamePrefix();
    String getServiceIndentity();
    String getProviderDirectIp();
    int getProviderDirectPort();
    int getAppVersion();
    boolean isDisableChoice();
}
