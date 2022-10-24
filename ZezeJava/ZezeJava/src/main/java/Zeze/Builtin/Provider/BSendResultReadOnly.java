// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSendResultReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSendResult copy();

    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getErrorLinkSidsReadOnly();
}
