// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// Task的总表，包含了所有的Task的Bean
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tTask extends TableX<Zeze.Builtin.Game.TaskBase.BTaskKey, Zeze.Builtin.Game.TaskBase.BTask>
        implements TableReadOnly<Zeze.Builtin.Game.TaskBase.BTaskKey, Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> {
    public tTask() {
        super("Zeze_Builtin_Game_TaskBase_tTask");
    }

    @Override
    public int getId() {
        return -563139393;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_TaskName = 1;
    public static final int VAR_TaskType = 2;
    public static final int VAR_State = 3;
    public static final int VAR_CurrentPhaseId = 4;
    public static final int VAR_TaskPhases = 5;
    public static final int VAR_PreTasks = 6;
    public static final int VAR_TaskCustomData = 7;

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
    public Zeze.Builtin.Game.TaskBase.BTask newValue() {
        return new Zeze.Builtin.Game.TaskBase.BTask();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BTaskReadOnly getReadOnly(Zeze.Builtin.Game.TaskBase.BTaskKey key) {
        return get(key);
    }
}