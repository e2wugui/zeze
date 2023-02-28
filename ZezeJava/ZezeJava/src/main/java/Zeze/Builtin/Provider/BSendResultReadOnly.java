// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSendResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSendResult copy();

    Zeze.Transaction.Collections.PList1ReadOnly<Long> getErrorLinkSidsReadOnly();
}
