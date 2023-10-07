// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BConditionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCondition copy();

    long getBeginTime();
    long getEndTime();
    Zeze.Transaction.Collections.PList1ReadOnly<String> getWordsReadOnly();
    int getContainsType();
    String getPattern();
}
