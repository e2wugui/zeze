// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// TaskPhase的Bean数据，只存在在BTask之内
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -2081342941887981883L;

    private long _TaskPhaseId;
    private long _CurrentConditionId;
    private final Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.Task.BTaskCondition> _TaskConditions;
    private final Zeze.Transaction.DynamicBean _TaskPhaseCustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TaskPhaseCustomData() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Game.Task::getSpecialTypeIdFromBean, Zeze.Game.Task::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean bean) {
        return Zeze.Game.Task.getSpecialTypeIdFromBean(bean);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long typeId) {
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
    public long getTaskPhaseId() {
        if (!isManaged())
            return _TaskPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskPhaseId;
        var log = (Log__TaskPhaseId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _TaskPhaseId;
    }

    public void setTaskPhaseId(long value) {
        if (!isManaged()) {
            _TaskPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskPhaseId(this, 1, value));
    }

    @Override
    public long getCurrentConditionId() {
        if (!isManaged())
            return _CurrentConditionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CurrentConditionId;
        var log = (Log__CurrentConditionId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _CurrentConditionId;
    }

    public void setCurrentConditionId(long value) {
        if (!isManaged()) {
            _CurrentConditionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CurrentConditionId(this, 2, value));
    }

    public Zeze.Transaction.Collections.PMap2<Long, Zeze.Builtin.Game.Task.BTaskCondition> getTaskConditions() {
        return _TaskConditions;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, Zeze.Builtin.Game.Task.BTaskCondition, Zeze.Builtin.Game.Task.BTaskConditionReadOnly> getTaskConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_TaskConditions);
    }

    public Zeze.Transaction.DynamicBean getTaskPhaseCustomData() {
        return _TaskPhaseCustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTaskPhaseCustomDataReadOnly() {
        return _TaskPhaseCustomData;
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _TaskConditions = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.Task.BTaskCondition.class);
        _TaskConditions.variableId(3);
        _TaskPhaseCustomData = newDynamicBean_TaskPhaseCustomData();
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(long _TaskPhaseId_, long _CurrentConditionId_) {
        _TaskPhaseId = _TaskPhaseId_;
        _CurrentConditionId = _CurrentConditionId_;
        _TaskConditions = new Zeze.Transaction.Collections.PMap2<>(Long.class, Zeze.Builtin.Game.Task.BTaskCondition.class);
        _TaskConditions.variableId(3);
        _TaskPhaseCustomData = newDynamicBean_TaskPhaseCustomData();
    }

    public void assign(BTaskPhase other) {
        setTaskPhaseId(other.getTaskPhaseId());
        setCurrentConditionId(other.getCurrentConditionId());
        _TaskConditions.clear();
        for (var e : other._TaskConditions.entrySet())
            _TaskConditions.put(e.getKey(), e.getValue().copy());
        _TaskPhaseCustomData.assign(other._TaskPhaseCustomData);
    }

    @Deprecated
    public void Assign(BTaskPhase other) {
        assign(other);
    }

    public BTaskPhase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTaskPhase copy() {
        var copy = new BTaskPhase();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTaskPhase Copy() {
        return copy();
    }

    public static void swap(BTaskPhase a, BTaskPhase b) {
        BTaskPhase save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskPhaseId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._TaskPhaseId = value; }
    }

    private static final class Log__CurrentConditionId extends Zeze.Transaction.Logs.LogLong {
        public Log__CurrentConditionId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._CurrentConditionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BTaskPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhaseId=").append(getTaskPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("CurrentConditionId=").append(getCurrentConditionId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditions={");
        if (!_TaskConditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _kv_ : _TaskConditions.entrySet()) {
                sb.append(Zeze.Util.Str.indent(level)).append("Key=").append(_kv_.getKey()).append(',').append(System.lineSeparator());
                sb.append(Zeze.Util.Str.indent(level)).append("Value=").append(System.lineSeparator());
                _kv_.getValue().buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append('}').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhaseCustomData=").append(System.lineSeparator());
        _TaskPhaseCustomData.getBean().buildString(sb, level + 4);
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
            long _x_ = getTaskPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getCurrentConditionId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _TaskConditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                }
            }
        }
        {
            var _x_ = _TaskPhaseCustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
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
            setTaskPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCurrentConditionId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _TaskConditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new Zeze.Builtin.Game.Task.BTaskCondition(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_TaskPhaseCustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        _TaskConditions.initRootInfo(root, this);
        _TaskPhaseCustomData.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _TaskConditions.resetRootInfo();
        _TaskPhaseCustomData.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskPhaseId() < 0)
            return true;
        if (getCurrentConditionId() < 0)
            return true;
        for (var _v_ : _TaskConditions.values()) {
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
                case 1: _TaskPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _CurrentConditionId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _TaskConditions.followerApply(vlog); break;
                case 4: _TaskPhaseCustomData.followerApply(vlog); break;
            }
        }
    }
}
