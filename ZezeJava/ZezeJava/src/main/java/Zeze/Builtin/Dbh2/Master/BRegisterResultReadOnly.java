// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BRegisterResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRegisterResult copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Dbh2.Master.BDbh2Config, Zeze.Builtin.Dbh2.Master.BDbh2ConfigReadOnly> getDbh2ConfigsReadOnly();
}
