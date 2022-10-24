// auto-generated @formatter:off
package Zeze.Builtin.Web;

public interface BHeaderReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BHeader copy();

    public Zeze.Transaction.Collections.PList1ReadOnly<String> getValuesReadOnly();
}
