// auto-generated @formatter:off
package demo.web;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BMap2Bean1ReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BMap2Bean1 copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, demo.Bean1, demo.Bean1ReadOnly> getMap2ReadOnly();
}
