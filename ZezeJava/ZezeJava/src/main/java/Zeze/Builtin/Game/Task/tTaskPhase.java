// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tTaskPhase extends TableX<Zeze.Builtin.Game.Task.BTaskPhaseKey, Zeze.Builtin.Game.Task.BTaskPhase>
        implements TableReadOnly<Zeze.Builtin.Game.Task.BTaskPhaseKey, Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> {
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

    public static final int VAR_TaskPhaseId = 1;
    public static final int VAR_TaskPhaseName = 2;
    public static final int VAR_CurrentCondition = 3;
    public static final int VAR_TaskPhaseCustomData = 4;

    @Override
    public Zeze.Builtin.Game.Task.BTaskPhaseKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Game.Task.BTaskPhaseKey _v_ = new Zeze.Builtin.Game.Task.BTaskPhaseKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.Task.BTaskPhaseKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskPhase newValue() {
        return new Zeze.Builtin.Game.Task.BTaskPhase();
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskPhaseReadOnly getReadOnly(Zeze.Builtin.Game.Task.BTaskPhaseKey key) {
        return get(key);
    }
}
