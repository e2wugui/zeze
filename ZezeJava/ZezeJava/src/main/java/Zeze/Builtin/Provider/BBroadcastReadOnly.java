// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BBroadcastReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBroadcast copy();

    long getProtocolType();
    Zeze.Net.Binary getProtocolWholeData();
    int getTime();
    boolean isOnlySameVersion();
}
