// auto-generated @formatter:off
package demo.Module1;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// bean的注释
public interface BAutoValueReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BAutoValue copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getCurrent();
    String getName();
    long getLocalId();
}
