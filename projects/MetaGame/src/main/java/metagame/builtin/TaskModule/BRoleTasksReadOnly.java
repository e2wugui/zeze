// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BRoleTasksReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BRoleTasks copy();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Transaction.VariableMeta> variables();

    Zeze.Transaction.Collections.PMap2ReadOnly<Integer, metagame.builtin.TaskModule.BTask, metagame.builtin.TaskModule.BTaskReadOnly> getTasksReadOnly();
}
