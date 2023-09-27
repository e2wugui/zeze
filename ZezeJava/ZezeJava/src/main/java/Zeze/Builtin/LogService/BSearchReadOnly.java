// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BSearchReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSearch copy();

    long getId();
    int getLimit();
    boolean isReset();
    Zeze.Builtin.LogService.BConditionReadOnly getConditionReadOnly();
}
