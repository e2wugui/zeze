// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tTaskPhase extends TableX<Zeze.Builtin.Game.Task.BTaskKey, Zeze.Builtin.Game.Task.BTask>
        implements TableReadOnly<Zeze.Builtin.Game.Task.BTaskKey, Zeze.Builtin.Game.Task.BTask, Zeze.Builtin.Game.Task.BTaskReadOnly> {
    public tTaskPhase() {
        super("Zeze_Builtin_Game_Task_tTaskPhase");
    }

    @Override
    public int getId() {
        return 1761056010;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_TaskId = 1;
    public static final int VAR_TaskName = 2;
    public static final int VAR_CurrentPhase = 3;
    public static final int VAR_TaskPhases = 4;
    public static final int VAR_TaskCustomData = 5;

    @Override
    public Zeze.Builtin.Game.Task.BTaskKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Game.Task.BTaskKey _v_ = new Zeze.Builtin.Game.Task.BTaskKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.Task.BTaskKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Task.BTask newValue() {
        return new Zeze.Builtin.Game.Task.BTask();
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskReadOnly getReadOnly(Zeze.Builtin.Game.Task.BTaskKey key) {
        return get(key);
    }
}
