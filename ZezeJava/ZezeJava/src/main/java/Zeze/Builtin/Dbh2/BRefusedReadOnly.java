// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BRefusedReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRefused copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Dbh2.BBatch, Zeze.Builtin.Dbh2.BBatchReadOnly> getRefusedReadOnly();
}
