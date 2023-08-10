// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// 记录每个角色的任务数据
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BRoleTasks extends Zeze.Transaction.Bean implements BRoleTasksReadOnly {
    public static final long TYPEID = -8731204756868645772L;

    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BTask> _processingTasks; // 处理中的任务（不可接、可接、正在进行、可提交等所有处理中的任务）
    private final Zeze.Transaction.Collections.PSet1<Long> _finishedTaskIds; // 已经完成封存的任务

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.TaskBase.BTask> getProcessingTasks() {
        return _processingTasks;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.TaskBase.BTask, Zeze.Builtin.Game.TaskBase.BTaskReadOnly> getProcessingTasksReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_processingTasks);
    }

    public Zeze.Transaction.Collections.PSet1<Long> getFinishedTaskIds() {
        return _finishedTaskIds;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getFinishedTaskIdsReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_finishedTaskIds);
    }

    @SuppressWarnings("deprecation")
    public BRoleTasks() {
        _processingTasks = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.TaskBase.BTask.class);
        _processingTasks.variableId(1);
        _finishedTaskIds = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _finishedTaskIds.variableId(2);
    }

    @Override
    public void reset() {
        _processingTasks.clear();
        _finishedTaskIds.clear();
        _unknown_ = null;
    }

    public void assign(BRoleTasks other) {
        _processingTasks.clear();
        for (var e : other._processingTasks.entrySet())
            _processingTasks.put(e.getKey(), e.getValue().copy());
        _finishedTaskIds.clear();
        _finishedTaskIds.addAll(other._finishedTaskIds);
        _unknown_ = other._unknown_;
    }

    public BRoleTasks copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BRoleTasks copy() {
        var copy = new BRoleTasks();
        copy.assign(this);
        return copy;
    }

    public static void swap(BRoleTasks a, BRoleTasks b) {
        BRoleTasks save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BRoleTasks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("processingTasks={");
        if (!_processingTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _processingTasks.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("finishedTaskIds={");
        if (!_finishedTaskIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _finishedTaskIds) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            var _x_ = _processingTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _finishedTaskIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _processingTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BTask(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _finishedTaskIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _processingTasks.initRootInfo(root, this);
        _finishedTaskIds.initRootInfo(root, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo root) {
        _processingTasks.initRootInfoWithRedo(root, this);
        _finishedTaskIds.initRootInfoWithRedo(root, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _processingTasks.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _finishedTaskIds) {
            if (_v_ < 0)
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log log) {
        var vars = ((Zeze.Transaction.Collections.LogBean)log).getVariables();
        if (vars == null)
            return;
        for (var it = vars.iterator(); it.moveToNext(); ) {
            var vlog = it.value();
            switch (vlog.getVariableId()) {
                case 1: _processingTasks.followerApply(vlog); break;
                case 2: _finishedTaskIds.followerApply(vlog); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        Zeze.Serialize.Helper.decodeJsonMap(this, "processingTasks", _processingTasks, rs.getString(_parents_name_ + "processingTasks"));
        Zeze.Serialize.Helper.decodeJsonSet(_finishedTaskIds, Long.class, rs.getString(_parents_name_ + "finishedTaskIds"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "processingTasks", Zeze.Serialize.Helper.encodeJson(_processingTasks));
        st.appendString(_parents_name_ + "finishedTaskIds", Zeze.Serialize.Helper.encodeJson(_finishedTaskIds));
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BRoleTasks
    }
}
