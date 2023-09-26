// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BRegexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRegex copy();

    long getBeginTime();
    long getEndTime();
    String getPattern();
}
