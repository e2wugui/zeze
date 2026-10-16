// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 保留这个结构，以后可能需要返回结果
public interface BFuncSagaEndResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BFuncSagaEndResult copy();
    BFuncSagaEndResult.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();
}
