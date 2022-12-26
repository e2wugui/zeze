// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSubPhase extends Zeze.Transaction.Bean implements BSubPhaseReadOnly {
    public static final long TYPEID = 621285588433689865L;

    private long _subPhaseId;
    private String _completeType; // CompleteAll/CompleteAny
    private long _nextSubPhaseId; // 下一个SubPhaseId（允许通过不同的conditions完成情况动态变化）
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BTaskCondition> _conditions; // 该SubPhase包含的所有的Condition

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
    public long getSubPhaseId() {
        if (!isManaged())
            return _subPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _subPhaseId;
        var log = (Log__subPhaseId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _subPhaseId;
    }

    public void setSubPhaseId(long value) {
        if (!isManaged()) {
            _subPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__subPhaseId(this, 1, value));
    }

    @Override
    public String getCompleteType() {
        if (!isManaged())
            return _completeType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _completeType;
        var log = (Log__completeType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _completeType;
    }

    public void setCompleteType(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _completeType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__completeType(this, 2, value));
    }

    @Override
    public long getNextSubPhaseId() {
        if (!isManaged())
            return _nextSubPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _nextSubPhaseId;
        var log = (Log__nextSubPhaseId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _nextSubPhaseId;
    }

    public void setNextSubPhaseId(long value) {
        if (!isManaged()) {
            _nextSubPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__nextSubPhaseId(this, 3, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BTaskCondition> getConditions() {
        return _conditions;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BTaskCondition, Zeze.Builtin.Game.TaskBase.BTaskConditionReadOnly> getConditionsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_conditions);
    }

    @SuppressWarnings("deprecation")
    public BSubPhase() {
        _completeType = "";
        _conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BTaskCondition.class);
        _conditions.variableId(4);
    }

    @SuppressWarnings("deprecation")
    public BSubPhase(long _subPhaseId_, String _completeType_, long _nextSubPhaseId_) {
        _subPhaseId = _subPhaseId_;
        if (_completeType_ == null)
            throw new IllegalArgumentException();
        _completeType = _completeType_;
        _nextSubPhaseId = _nextSubPhaseId_;
        _conditions = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BTaskCondition.class);
        _conditions.variableId(4);
    }

    public void assign(BSubPhase other) {
        setSubPhaseId(other.getSubPhaseId());
        setCompleteType(other.getCompleteType());
        setNextSubPhaseId(other.getNextSubPhaseId());
        _conditions.clear();
        for (var e : other._conditions)
            _conditions.add(e.copy());
    }

    @Deprecated
    public void Assign(BSubPhase other) {
        assign(other);
    }

    public BSubPhase copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSubPhase copy() {
        var copy = new BSubPhase();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BSubPhase Copy() {
        return copy();
    }

    public static void swap(BSubPhase a, BSubPhase b) {
        BSubPhase save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__subPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__subPhaseId(BSubPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSubPhase)getBelong())._subPhaseId = value; }
    }

    private static final class Log__completeType extends Zeze.Transaction.Logs.LogString {
        public Log__completeType(BSubPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSubPhase)getBelong())._completeType = value; }
    }

    private static final class Log__nextSubPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__nextSubPhaseId(BSubPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSubPhase)getBelong())._nextSubPhaseId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BSubPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("subPhaseId=").append(getSubPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("completeType=").append(getCompleteType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("nextSubPhaseId=").append(getNextSubPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("conditions=[");
        if (!_conditions.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _conditions) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(System.lineSeparator());
                _item_.buildString(sb, level + 4);
                sb.append(',').append(System.lineSeparator());
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
            long _x_ = getSubPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getCompleteType();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getNextSubPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _conditions;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_)
                    _v_.encode(_o_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setSubPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCompleteType(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setNextSubPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            var _x_ = _conditions;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BTaskCondition(), _t_));
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
        _conditions.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _conditions.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getSubPhaseId() < 0)
            return true;
        if (getNextSubPhaseId() < 0)
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
                case 1: _subPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _completeType = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _nextSubPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _conditions.followerApply(vlog); break;
            }
        }
    }
}
