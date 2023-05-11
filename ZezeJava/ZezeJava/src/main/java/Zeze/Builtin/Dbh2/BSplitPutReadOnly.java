// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BSplitPutReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSplitPut copy();

    boolean isFromTransaction();
    Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Net.Binary, Zeze.Net.Binary> getPutsReadOnly();
}
