// auto-generated @formatter:off
package Zeze.Builtin.Token;

public interface BGetTokenArgReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BGetTokenArg copy();

    String getToken();
    long getMaxCount();
}
