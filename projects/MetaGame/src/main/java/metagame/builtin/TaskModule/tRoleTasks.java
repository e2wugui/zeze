// auto-generated @formatter:off
package metagame.builtin.TaskModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tRoleTasks extends TableX<Long, metagame.builtin.TaskModule.BRoleTasks>
        implements TableReadOnly<Long, metagame.builtin.TaskModule.BRoleTasks, metagame.builtin.TaskModule.BRoleTasksReadOnly> {
    public tRoleTasks() {
        super(-698112376, "metagame_builtin_TaskModule_tRoleTasks");
    }

    public tRoleTasks(String _s_) {
        super(-698112376, "metagame_builtin_TaskModule_tRoleTasks", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<metagame.builtin.TaskModule.BRoleTasks> getValueClass() {
        return metagame.builtin.TaskModule.BRoleTasks.class;
    }

    public static final int VAR_Tasks = 1;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public metagame.builtin.TaskModule.BRoleTasks newValue() {
        return new metagame.builtin.TaskModule.BRoleTasks();
    }

    @Override
    public metagame.builtin.TaskModule.BRoleTasksReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
