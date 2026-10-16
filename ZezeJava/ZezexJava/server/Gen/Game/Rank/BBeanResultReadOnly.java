// auto-generated @formatter:off
package Game.Rank;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 这个Rank代码全部删除，see Zeze.Game.Rank，这里只剩下redirect的一些测试。
public interface BBeanResultReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BBeanResult copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    int getInti();
}
