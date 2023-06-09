// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BQueryLockInfoReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueryLockInfo copy();

    Zeze.Transaction.Collections.PList1ReadOnly<String> getLockNamesReadOnly();
}
