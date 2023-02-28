// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BSendReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSend copy();

    Zeze.Transaction.Collections.PList1ReadOnly<Long> getLinkSidsReadOnly();
    long getProtocolType();
    Zeze.Net.Binary getProtocolWholeData();
}
