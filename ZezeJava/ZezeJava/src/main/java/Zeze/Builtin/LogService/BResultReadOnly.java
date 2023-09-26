// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public interface BResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BResult copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.LogService.BLog, Zeze.Builtin.LogService.BLogReadOnly> getLogsReadOnly();
    boolean isRemain();
}
