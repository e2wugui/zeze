// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Task的总表，包含了所有的Task的Bean
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tTask extends TableX<Zeze.Builtin.Game.TaskBase.BTaskKey, Zeze.Builtin.Game.TaskBase.BTask>
        implements TableReadOnly<Zeze.Builtin.Game.TaskBase.BTaskKey, Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> {
    public tTask() {
        super(-563139393, "Zeze_Builtin_Game_TaskBase_tTask");
    }

    public tTask(String suffix) {
        super(-563139393, "Zeze_Builtin_Game_TaskBase_tTask", suffix);
    }

    public static final int VAR_roleId = 1;
    public static final int VAR_taskId = 2;
    public static final int VAR_taskType = 3;
    public static final int VAR_taskState = 4;
    public static final int VAR_taskName = 5;
    public static final int VAR_taskDescription = 6;
    public static final int VAR_preTaskIds = 7;
    public static final int VAR_currentPhaseId = 8;
    public static final int VAR_taskPhases = 9;
    public static final int VAR_extendedData = 10;

    @Override
    public Zeze.Builtin.Game.TaskBase.BTaskKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Game.TaskBase.BTaskKey _v_ = new Zeze.Builtin.Game.TaskBase.BTaskKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.TaskBase.BTaskKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BTaskKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.Game.TaskBase.BTaskKey _v_ = new Zeze.Builtin.Game.TaskBase.BTaskKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.Game.TaskBase.BTaskKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BTask newValue() {
        return new Zeze.Builtin.Game.TaskBase.BTask();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BTaskReadOnly getReadOnly(Zeze.Builtin.Game.TaskBase.BTaskKey key) {
        return get(key);
    }
}
