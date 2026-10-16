// auto-generated @formatter:off
package demo;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// bean的注释
public interface Bean1ReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    Bean1 copy();
    Bean1.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getV1();
    Zeze.Transaction.Collections.PMap1ReadOnly<Integer, Integer> getV2ReadOnly();
}
