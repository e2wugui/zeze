// auto-generated @formatter:off
package Zeze.Builtin.LinkdBase;

// linkd to client
public interface BReportErrorReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BReportError copy();

    public int getFrom();
    public int getCode();
    public String getDesc();
}
