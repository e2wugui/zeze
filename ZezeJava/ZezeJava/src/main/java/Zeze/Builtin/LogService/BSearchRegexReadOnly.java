// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BSearchRegexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSearchRegex copy();

    long getBeginTime();
    long getEndTime();
    String getPattern();
}
