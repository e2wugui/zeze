// auto-generated @formatter:off
package TaskTest.TaskExt;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 扩展的条件 Extended Condition
public interface BTConditionExploreWorldReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BTConditionExploreWorld copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    double getExploreRate();
    boolean isFinished();
}
