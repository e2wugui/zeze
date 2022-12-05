// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSendReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSend copy();

    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getLinkSidsReadOnly();
    public long getProtocolType();
    public Zeze.Net.Binary getProtocolWholeData();
}
