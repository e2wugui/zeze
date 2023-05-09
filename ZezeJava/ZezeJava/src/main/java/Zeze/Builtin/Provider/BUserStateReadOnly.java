// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BUserStateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BUserState copy();

    String getContext();
    Zeze.Net.Binary getContextx();
    String getOnlineSetName();
    long getLoginVersion();
}
