// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public interface BBatchReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BBatch copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Net.Binary, Zeze.Net.Binary> getPutsReadOnly();
    Zeze.Transaction.Collections.PSet1ReadOnly<Zeze.Net.Binary> getDeletesReadOnly();
    String getQueryIp();
    int getQueryPort();
    long getTid();
}
