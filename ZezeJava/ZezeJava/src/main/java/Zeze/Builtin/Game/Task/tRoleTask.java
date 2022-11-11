// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tRoleTask extends TableX<Long, Zeze.Builtin.Game.Task.BPTaskInfo>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Task.BPTaskInfo, Zeze.Builtin.Game.Task.BPTaskInfoReadOnly> {
    public tRoleTask() {
        super("Zeze_Builtin_Game_Task_tRoleTask");
    }

    @Override
    public int getId() {
        return -1706187955;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_processingTask = 1;
    public static final int VAR_finishedTask = 2;
    public static final int VAR_RoleTaskCustomData = 3;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Task.BPTaskInfo newValue() {
        return new Zeze.Builtin.Game.Task.BPTaskInfo();
    }

    @Override
    public Zeze.Builtin.Game.Task.BPTaskInfoReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
