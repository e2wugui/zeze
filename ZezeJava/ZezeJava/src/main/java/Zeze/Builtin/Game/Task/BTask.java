// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTask extends Zeze.Transaction.Bean implements BTaskReadOnly {
    public static final long TYPEID = -3876171389489280800L;

    private long _TaskId;
    private int _State;
    private long _CurrentPhaseId;
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.Task.BTaskPhase> _TaskPhases;
    private final Zeze.Transaction.DynamicBean _TaskCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskCustomData() {
        return new Zeze.Transaction.DynamicBean(5, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_5(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_5(long typeId) {
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
    public long getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskId;
        var log = (Log__TaskId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskId(this, 1, value));
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
    public long getCurrentPhaseId() {
        if (!isManaged())
            return _CurrentPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CurrentPhaseId;
        var log = (Log__CurrentPhaseId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _CurrentPhaseId;
    }

    public void setCurrentPhaseId(long value) {
        if (!isManaged()) {
            _CurrentPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CurrentPhaseId(this, 3, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.Task.BTaskPhase> getTaskPhases() {
        return _TaskPhases;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.Task.BTaskPhase, Zeze.Builtin.Game.Task.BTaskPhaseReadOnly> getTaskPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_TaskPhases);
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
        _TaskPhases = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.Task.BTaskPhase.class);
        _TaskPhases.variableId(4);
        _TaskCustomData = newDynamicBean_TaskCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTask(long _TaskId_, int _State_, long _CurrentPhaseId_) {
        _TaskId = _TaskId_;
        _State = _State_;
        _CurrentPhaseId = _CurrentPhaseId_;
        _TaskPhases = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.Task.BTaskPhase.class);
        _TaskPhases.variableId(4);
        _TaskCustomData = newDynamicBean_TaskCustomData();
    }

    public void assign(BTask other) {
        setTaskId(other.getTaskId());
        setState(other.getState());
        setCurrentPhaseId(other.getCurrentPhaseId());
        _TaskPhases.clear();
        for (var e : other._TaskPhases.entrySet())
            _TaskPhases.put(e.getKey(), e.getValue().copy());
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

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskId(BTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._TaskId = value; }
    }

    private static final class Log__State extends Zeze.Transaction.Logs.LogInt {
        public Log__State(BTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTask)getBelong())._State = value; }
    }

    private static final class Log__CurrentPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__CurrentPhaseId(BTask bean, int varId, long value) { super(bean, varId, value); }

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
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId=").append(getTaskId()).append(',').append(System.lineSeparator());
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
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
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
            long _x_ = getCurrentPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _TaskPhases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
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
            setTaskId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setCurrentPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _TaskPhases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Task.BTaskPhase(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
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
        _TaskPhases.initRootInfo(root, this);
        _TaskCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TaskPhases.resetRootInfo();
        _TaskCustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
            return true;
        if (getState() < 0)
            return true;
        if (getCurrentPhaseId() < 0)
            return true;
        for (var _v_ : _TaskPhases.values()) {
            if (_v_.negativeCheck())
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
                case 1: _TaskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _State = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _CurrentPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _TaskPhases.followerApply(vlog); break;
                case 5: _TaskCustomData.followerApply(vlog); break;
            }
        }
    }
}
