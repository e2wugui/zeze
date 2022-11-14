// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BPTaskInfo extends Zeze.Transaction.Bean implements BPTaskInfoReadOnly {
    public static final long TYPEID = -141577318142513096L;

    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.Task.BTask> _processingTask;
    private final Zeze.Transaction.Collections.PSet1<Long> _finishedTask; // 已经结束的任务，不存储可重复完成的任务
    private final Zeze.Transaction.DynamicBean _RoleTaskCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_RoleTaskCustomData() {
        return new Zeze.Transaction.DynamicBean(3, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long typeId) {
        return Zeze.Game.Task.createBeanFromSpecialTypeId(typeId);
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.Task.BTask> getProcessingTask() {
        return _processingTask;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.Task.BTask, Zeze.Builtin.Game.Task.BTaskReadOnly> getProcessingTaskReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_processingTask);
    }

    public Zeze.Transaction.Collections.PSet1<Long> getFinishedTask() {
        return _finishedTask;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getFinishedTaskReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_finishedTask);
    }

    public Zeze.Transaction.DynamicBean getRoleTaskCustomData() {
        return _RoleTaskCustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getRoleTaskCustomDataReadOnly() {
        return _RoleTaskCustomData;
    }

    @SuppressWarnings("deprecation")
    public BPTaskInfo() {
        _processingTask = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.Task.BTask.class);
        _processingTask.variableId(1);
        _finishedTask = new Zeze.Transaction.Collections.PSet1<>(Long.class);
        _finishedTask.variableId(2);
        _RoleTaskCustomData = newDynamicBean_RoleTaskCustomData();
    }

    public void assign(BPTaskInfo other) {
        _processingTask.clear();
        for (var e : other._processingTask.entrySet())
            _processingTask.put(e.getKey(), e.getValue().copy());
        _finishedTask.clear();
        _finishedTask.addAll(other._finishedTask);
        _RoleTaskCustomData.assign(other._RoleTaskCustomData);
    }

    @Deprecated
    public void Assign(BPTaskInfo other) {
        assign(other);
    }

    public BPTaskInfo copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BPTaskInfo copy() {
        var copy = new BPTaskInfo();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BPTaskInfo Copy() {
        return copy();
    }

    public static void swap(BPTaskInfo a, BPTaskInfo b) {
        BPTaskInfo save = a.copy();
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
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BPTaskInfo: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("processingTask").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _kv_ : _processingTask.entrySet()) {
            sb.append(Zeze.Util.Str.indent(level)).append('(').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Key").append('=').append(_kv_.getKey()).append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append("Value").append('=').append(System.lineSeparator());
            _kv_.getValue().buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
            sb.append(Zeze.Util.Str.indent(level)).append(')').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("finishedTask").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : _finishedTask) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(_item_).append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RoleTaskCustomData").append('=').append(System.lineSeparator());
        _RoleTaskCustomData.getBean().buildString(sb, level + 4);
        sb.append(System.lineSeparator());
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
            var _x_ = _processingTask;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            var _x_ = _finishedTask;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            var _x_ = _RoleTaskCustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _processingTask;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Task.BTask(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _finishedTask;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_RoleTaskCustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _processingTask.initRootInfo(root, this);
        _finishedTask.initRootInfo(root, this);
        _RoleTaskCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _processingTask.resetRootInfo();
        _finishedTask.resetRootInfo();
        _RoleTaskCustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _processingTask.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _finishedTask) {
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
                case 1: _processingTask.followerApply(vlog); break;
                case 2: _finishedTask.followerApply(vlog); break;
                case 3: _RoleTaskCustomData.followerApply(vlog); break;
            }
        }
    }
}
