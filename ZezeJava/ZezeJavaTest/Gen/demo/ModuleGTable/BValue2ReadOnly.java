// auto-generated @formatter:off
package demo.ModuleGTable;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BValue2ReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BValue2 copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, Zeze.Transaction.GTable.BeanMap2<Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly>, Zeze.Transaction.GTable.BeanMap2ReadOnly<Integer, demo.ModuleGTable.Bean1, demo.ModuleGTable.Bean1ReadOnly>> getGTableReadOnly();
}
