// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTask extends Zeze.Transaction.Bean implements BTaskReadOnly {
    public static final long TYPEID = -3876171389489280800L;

    private String _TaskName; // 任务的名字，唯一
    private int _State; // 任务的状态，详见在上面的enum
    private String _CurrentPhaseId; // 当前的PhaseId
    private final Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Game.Task.BTaskPhase> _TaskPhases; // 该任务所有的Phase
    private final Zeze.Transaction.Collections.PList1<String> _PreTasks; // 前置任务名（只需要存储前置任务就可以建立起整个TaskGraphics，不需要存储后置任务）
    private final Zeze.Transaction.DynamicBean _TaskCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskCustomData() {
        return new Zeze.Transaction.DynamicBean(6, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_6(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_6(long typeId) {
        return Zeze.Game.Task.createBeanFromSpecialTypeId(typeId);
    }

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object value) {
        __zeze_map_key__ = value;
    }

    @Override
    public String getTaskName() {
        if (!isManaged())
            return _TaskName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskName;
        var log = (Log__TaskName)txn.getLog(objectId() + 1);
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
        txn.putLog(new Log__TaskName(this, 1, value));
    }

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _State;
        var log = (Log__State)txn.getLog(objectId() + 2);
        return log != null ? log.value : _State;
    }

    public void setState(int value) {
        if (!isManaged()) {
            _State = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__State(this, 2, value));
    }

    @Override
    public String getCurrentPhaseId() {
        if (!isManaged())
            return _CurrentPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CurrentPhaseId;
        var log = (Log__CurrentPhaseId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _CurrentPhaseId;
    }

    public void setCurrentPhaseId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CurrentPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CurrentPhaseId(this, 3, value));
    }

    public Zeze.Transaction.Collections.PMap2<String, Zeze.Builtin.Game.Task.BTaskPhase> getTaskPhases() {
        return _TaskPhases;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> getTaskPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_TaskPhases);
    }

    public Zeze.Transaction.Collections.PList1<String> getPreTasks() {
        return _PreTasks;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getPreTasksReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_PreTasks);
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
        _TaskName = "";
        _CurrentPhaseId = "";
        _TaskPhases = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Game.Task.BTaskPhase.class);
        _TaskPhases.variableId(4);
        _PreTasks = new Zeze.Transaction.Collections.PList1<>(String.class);
        _PreTasks.variableId(5);
        _TaskCustomData = newDynamicBean_TaskCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTask(String _TaskName_, int _State_, String _CurrentPhaseId_) {
        if (_TaskName_ == null)
            throw new IllegalArgumentException();
        _TaskName = _TaskName_;
        _State = _State_;
        if (_CurrentPhaseId_ == null)
            throw new IllegalArgumentException();
        _CurrentPhaseId = _CurrentPhaseId_;
        _TaskPhases = new Zeze.Transaction.Collections.PMap2<>(String.class, Zeze.Builtin.Game.Task.BTaskPhase.class);
        _TaskPhases.variableId(4);
        _PreTasks = new Zeze.Transaction.Collections.PList1<>(String.class);
        _PreTasks.variableId(5);
        _TaskCustomData = newDynamicBean_TaskCustomData();
    }

    public void assign(BTask other) {
        setTaskName(other.getTaskName());
        setState(other.getState());
        setCurrentPhaseId(other.getCurrentPhaseId());
        _TaskPhases.clear();
        for (var e : other._TaskPhases.entrySet())
            _TaskPhases.put(e.getKey(), e.getValue().copy());
        _PreTasks.clear();
        _PreTasks.addAll(other._PreTasks);
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

    private static final class Log__TaskName extends Zeze.Transaction.Logs.LogString {
        public Log__TaskName(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._TaskName = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._State = value; }
    }

    private static final class Log__CurrentPhaseId extends Zeze.Transaction.Logs.LogString {
        public Log__CurrentPhaseId(BTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._CurrentPhaseId = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("TaskName=").append(getTaskName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(getState()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CurrentPhaseId=").append(getCurrentPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhases={");
        if (!_TaskPhases.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _TaskPhases.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PreTasks=[");
        if (!_PreTasks.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _PreTasks) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskCustomData=").append(System.lineSeparator());
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
            String _x_ = getTaskName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getCurrentPhaseId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _TaskPhases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteString(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            var _x_ = _PreTasks;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_)
                    _o_.WriteString(_v_);
            }
        }
        {
            var _x_ = _TaskCustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.DYNAMIC);
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
            setTaskName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCurrentPhaseId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _TaskPhases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadString(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Task.BTaskPhase(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _PreTasks;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
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
        _TaskPhases.initRootInfo(root, this);
        _PreTasks.initRootInfo(root, this);
        _TaskCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TaskPhases.resetRootInfo();
        _PreTasks.resetRootInfo();
        _TaskCustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
            return true;
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
                case 1: _TaskName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _State = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _CurrentPhaseId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _TaskPhases.followerApply(vlog); break;
                case 5: _PreTasks.followerApply(vlog); break;
                case 6: _TaskCustomData.followerApply(vlog); break;
            }
        }
    }
}
