// auto-generated @formatter:off
package Zeze.Builtin.LinkdBase;

// linkd to client
public interface BReportErrorReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReportError copy();

    int getFrom();
    int getCode();
    String getDesc();
}
