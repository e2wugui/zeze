// auto-generated @formatter:off
package Game.Rank;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface TestToServerResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    TestToServerResult copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    long getResultCode();
    int getOut();
    int getServerId();
}
