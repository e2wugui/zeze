// auto-generated @formatter:off
package Zeze.Builtin.Token;

public interface BNewTokenArgReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BNewTokenArg copy();

    Zeze.Net.Binary getContext();
    long getTtl();
}
