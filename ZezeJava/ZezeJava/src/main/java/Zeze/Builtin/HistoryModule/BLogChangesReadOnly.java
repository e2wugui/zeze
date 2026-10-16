// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BLogChangesReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BLogChanges copy();
    BLogChanges.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Util.Id128 getGlobalSerialId();
    String getProtocolClassName();
    Zeze.Net.Binary getProtocolArgument();
    Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Builtin.HistoryModule.BTableKey, Zeze.Net.Binary> getChangesReadOnly();
    long getTimestamp();
}
