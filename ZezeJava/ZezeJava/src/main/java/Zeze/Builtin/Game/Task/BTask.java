// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTask extends Zeze.Transaction.Bean implements BTaskReadOnly {
    public static final long TYPEID = -3876171389489280800L;

    private String _TaskId;
    private String _TaskName;
        final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Game.Task.BTaskPhase> _CurrentPhase;
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.Task.BTaskPhase> _TaskPhases;
    private final Zeze.Transaction.DynamicBean _TaskCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskCustomData() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_TaskCustomData(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_TaskCustomData(long typeId) {
        return Zeze.Game.Task.createBeanFromSpecialTypeId(typeId);
    }

    @Override
    public String getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskId;
        var log = (Log__TaskId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskId(this, 1, value));
    }

    @Override
    public String getTaskName() {
        if (!isManaged())
            return _TaskName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskName;
        var log = (Log__TaskName)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TaskName;
    }

    public void setTaskName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskName(this, 2, value));
    }

    public Zeze.Builtin.Game.Task.BTaskPhase getCurrentPhase() {
        return _CurrentPhase.getValue();
    }

    public void setCurrentPhase(Zeze.Builtin.Game.Task.BTaskPhase value) {
        _CurrentPhase.setValue(value);
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskPhaseReadOnly getCurrentPhaseReadOnly() {
        return _CurrentPhase.getValue();
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.Task.BTaskPhase> getTaskPhases() {
        return _TaskPhases;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> getTaskPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_TaskPhases);
    }

    public Zeze.Transaction.DynamicBean getTaskCustomData() {
        return _TaskCustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTaskCustomDataReadOnly() {
        return _TaskCustomData;
    }

    @SuppressWarnings("deprecation")
    public BTask() {
        _TaskId = "";
        _TaskName = "";
        _CurrentPhase = new Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Game.Task.BTaskPhase>(new Zeze.Builtin.Game.Task.BTaskPhase(), Zeze.Builtin.Game.Task.BTaskPhase.class);
        _CurrentPhase.variableId(3);
        _TaskPhases = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.Task.BTaskPhase.class);
        _TaskPhases.variableId(4);
        _TaskCustomData = newDynamicBean_TaskCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTask(String _TaskId_, String _TaskName_) {
        if (_TaskId_ == null)
            throw new IllegalArgumentException();
        _TaskId = _TaskId_;
        if (_TaskName_ == null)
            throw new IllegalArgumentException();
        _TaskName = _TaskName_;
        _CurrentPhase = new Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Game.Task.BTaskPhase>(new Zeze.Builtin.Game.Task.BTaskPhase(), Zeze.Builtin.Game.Task.BTaskPhase.class);
        _CurrentPhase.variableId(3);
        _TaskPhases = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.Task.BTaskPhase.class);
        _TaskPhases.variableId(4);
        _TaskCustomData = newDynamicBean_TaskCustomData();
    }

    public void assign(BTask other) {
        setTaskId(other.getTaskId());
        setTaskName(other.getTaskName());
        _CurrentPhase.assign(other._CurrentPhase);
        _TaskPhases.clear();
        for (var e : other._TaskPhases)
            _TaskPhases.add(e.copy());
        _TaskCustomData.assign(other._TaskCustomData);
    }

    @Deprecated
    public void Assign(BTask other) {
        assign(other);
    }

    public BTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTask copy() {
        var copy = new BTask();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTask Copy() {
        return copy();
    }

    public static void swap(BTask a, BTask b) {
        BTask save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogString {
        public Log__TaskId(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskName extends Zeze.Transaction.Logs.LogString {
        public Log__TaskName(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._TaskName = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId").append('=').append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskName").append('=').append(getTaskName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CurrentPhase").append('=').append(System.lineSeparator());
        _CurrentPhase.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhases").append("=[").append(System.lineSeparator());
        level += 4;
        for (var _item_ : _TaskPhases) {
            sb.append(Zeze.Util.Str.indent(level)).append("Item").append('=').append(System.lineSeparator());
            _item_.buildString(sb, level + 4);
            sb.append(',').append(System.lineSeparator());
        }
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskCustomData").append('=').append(System.lineSeparator());
        _TaskCustomData.getBean().buildString(sb, level + 4);
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
            String _x_ = getTaskId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTaskName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 3, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _CurrentPhase.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _TaskPhases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.encode(_o_);
            }
        }
        {
            var _x_ = _TaskCustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.DYNAMIC);
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
            setTaskId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTaskName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadBean(_CurrentPhase, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _TaskPhases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.Task.BTaskPhase(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _o_.ReadDynamic(_TaskCustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _CurrentPhase.initRootInfo(root, this);
        _TaskPhases.initRootInfo(root, this);
        _TaskCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _CurrentPhase.resetRootInfo();
        _TaskPhases.resetRootInfo();
        _TaskCustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
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
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _TaskName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _CurrentPhase.followerApply(vlog); break;
                case 4: _TaskPhases.followerApply(vlog); break;
                case 5: _TaskCustomData.followerApply(vlog); break;
            }
        }
    }
}
