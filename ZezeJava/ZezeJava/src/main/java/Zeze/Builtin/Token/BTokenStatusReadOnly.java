// auto-generated @formatter:off
package Zeze.Builtin.Token;

public interface BTokenStatusReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTokenStatus copy();

    long getNewCount();
    long getCurCount();
    int getConnectCount();
}
