// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tRoleTask extends TableX<Long, Zeze.Builtin.Game.TaskBase.BRoleTasks>
        implements TableReadOnly<Long, Zeze.Builtin.Game.TaskBase.BRoleTasks, Zeze.Builtin.Game.TaskBase.BRoleTasksReadOnly> {
    public tRoleTask() {
        super("Zeze_Builtin_Game_TaskBase_tRoleTask");
    }

    public tRoleTask(String suffix) {
        super("Zeze_Builtin_Game_TaskBase_tRoleTask" + suffix);
    }

    public String getOriginName() {
        return "Zeze_Builtin_Game_TaskBase_tRoleTask";
    }

    @Override
    public int getId() {
        return 192055248;
    }

    public static final int VAR_processingTasks = 1;
    public static final int VAR_finishedTaskIds = 2;

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
    public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        long _v_;
        _v_ = rs.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long _v_) {
        st.appendLong("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BRoleTasks newValue() {
        return new Zeze.Builtin.Game.TaskBase.BRoleTasks();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BRoleTasksReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
