// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BRegisterResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BRegisterResult copy();
    BRegisterResult.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Dbh2.Master.BDbh2Config, Zeze.Builtin.Dbh2.Master.BDbh2ConfigReadOnly> getDbh2ConfigsReadOnly();
}
