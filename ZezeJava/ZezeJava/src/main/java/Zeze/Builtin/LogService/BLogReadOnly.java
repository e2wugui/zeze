// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BLogReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLog copy();

    long getTime();
    String getLog();
}
