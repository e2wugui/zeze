// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// 记录每个角色的任务数据
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class RoleTasks extends Zeze.Transaction.Bean implements RoleTasksReadOnly {
    public static final long TYPEID = 55619011865561918L;

    private final Zeze.Transaction.Collections.PList1<Long> _availableTasksId;
    private final Zeze.Transaction.Collections.PList1<Long> _processingTasksId;
    private final Zeze.Transaction.Collections.PList1<Long> _finishedTaskId;

    public Zeze.Transaction.Collections.PList1<Long> getAvailableTasksId() {
        return _availableTasksId;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getAvailableTasksIdReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_availableTasksId);
    }

    public Zeze.Transaction.Collections.PList1<Long> getProcessingTasksId() {
        return _processingTasksId;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getProcessingTasksIdReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_processingTasksId);
    }

    public Zeze.Transaction.Collections.PList1<Long> getFinishedTaskId() {
        return _finishedTaskId;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getFinishedTaskIdReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_finishedTaskId);
    }

    @SuppressWarnings("deprecation")
    public RoleTasks() {
        _availableTasksId = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _availableTasksId.variableId(1);
        _processingTasksId = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _processingTasksId.variableId(2);
        _finishedTaskId = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _finishedTaskId.variableId(3);
    }

    public void assign(RoleTasks other) {
        _availableTasksId.clear();
        _availableTasksId.addAll(other._availableTasksId);
        _processingTasksId.clear();
        _processingTasksId.addAll(other._processingTasksId);
        _finishedTaskId.clear();
        _finishedTaskId.addAll(other._finishedTaskId);
    }

    @Deprecated
    public void Assign(RoleTasks other) {
        assign(other);
    }

    public RoleTasks copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public RoleTasks copy() {
        var copy = new RoleTasks();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public RoleTasks Copy() {
        return copy();
    }

    public static void swap(RoleTasks a, RoleTasks b) {
        RoleTasks save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.RoleTasks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("availableTasksId=[");
        if (!_availableTasksId.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _availableTasksId) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("processingTasksId=[");
        if (!_processingTasksId.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _processingTasksId) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("finishedTaskId=[");
        if (!_finishedTaskId.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _finishedTaskId) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(System.lineSeparator());
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = _availableTasksId;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            var _x_ = _processingTasksId;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            var _x_ = _finishedTaskId;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _availableTasksId;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _processingTasksId;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _finishedTaskId;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _availableTasksId.initRootInfo(root, this);
        _processingTasksId.initRootInfo(root, this);
        _finishedTaskId.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _availableTasksId.resetRootInfo();
        _processingTasksId.resetRootInfo();
        _finishedTaskId.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _availableTasksId) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _processingTasksId) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _finishedTaskId) {
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
                case 1: _availableTasksId.followerApply(vlog); break;
                case 2: _processingTasksId.followerApply(vlog); break;
                case 3: _finishedTaskId.followerApply(vlog); break;
            }
        }
    }
}
