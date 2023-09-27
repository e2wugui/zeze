// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BBrowseReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBrowse copy();

    long getId();
    int getLimit();
    float getOffsetFactor();
    boolean isReset();
    Zeze.Builtin.LogService.BConditionReadOnly getConditionReadOnly();
}
