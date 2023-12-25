// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BNewSessionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BNewSession copy();

    String getLogName();
}
