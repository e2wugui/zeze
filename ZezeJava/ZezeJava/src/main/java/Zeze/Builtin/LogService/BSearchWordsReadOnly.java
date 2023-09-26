// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BSearchWordsReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSearchWords copy();

    long getBeginTime();
    long getEndTime();
    Zeze.Transaction.Collections.PList1ReadOnly<String> getWordsReadOnly();
    boolean isContainsAll();
}
