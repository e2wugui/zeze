// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

// ======================================== TaskPhase的Bean数据 ========================================
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTaskPhase extends Zeze.Transaction.Bean implements BTaskPhaseReadOnly {
    public static final long TYPEID = -3008758867375693466L;

    private long _taskId; // Phase的Id
    private long _phaseId; // Phase的Id
    private String _phaseName; // Phase的名字
    private String _phaseDescription; // Phase的描述
    private final Zeze.Transaction.Collections.PList1<Long> _prePhaseIds; // 前置PhaseId
    private long _nextPhaseId; // 下一个PhaseId（允许通过不同的conditions完成情况动态变化）
    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BSubPhase> _subPhases; // 该Phase包含的所有的SubPhase

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
            return _taskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _taskId;
        var log = (Log__taskId)txn.getLog(objectId() + 1);
        return log != null ? log.value : _taskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _taskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__taskId(this, 1, value));
    }

    @Override
    public long getPhaseId() {
        if (!isManaged())
            return _phaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseId;
        var log = (Log__phaseId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _phaseId;
    }

    public void setPhaseId(long value) {
        if (!isManaged()) {
            _phaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseId(this, 2, value));
    }

    @Override
    public String getPhaseName() {
        if (!isManaged())
            return _phaseName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseName;
        var log = (Log__phaseName)txn.getLog(objectId() + 3);
        return log != null ? log.value : _phaseName;
    }

    public void setPhaseName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _phaseName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseName(this, 3, value));
    }

    @Override
    public String getPhaseDescription() {
        if (!isManaged())
            return _phaseDescription;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _phaseDescription;
        var log = (Log__phaseDescription)txn.getLog(objectId() + 4);
        return log != null ? log.value : _phaseDescription;
    }

    public void setPhaseDescription(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _phaseDescription = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__phaseDescription(this, 4, value));
    }

    public Zeze.Transaction.Collections.PList1<Long> getPrePhaseIds() {
        return _prePhaseIds;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getPrePhaseIdsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_prePhaseIds);
    }

    @Override
    public long getNextPhaseId() {
        if (!isManaged())
            return _nextPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _nextPhaseId;
        var log = (Log__nextPhaseId)txn.getLog(objectId() + 6);
        return log != null ? log.value : _nextPhaseId;
    }

    public void setNextPhaseId(long value) {
        if (!isManaged()) {
            _nextPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__nextPhaseId(this, 6, value));
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Game.TaskBase.BSubPhase> getSubPhases() {
        return _subPhases;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.TaskBase.BSubPhase, Zeze.Builtin.Game.TaskBase.BSubPhaseReadOnly> getSubPhasesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_subPhases);
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase() {
        _phaseName = "";
        _phaseDescription = "";
        _prePhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _prePhaseIds.variableId(5);
        _subPhases = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BSubPhase.class);
        _subPhases.variableId(7);
    }

    @SuppressWarnings("deprecation")
    public BTaskPhase(long _taskId_, long _phaseId_, String _phaseName_, String _phaseDescription_, long _nextPhaseId_) {
        _taskId = _taskId_;
        _phaseId = _phaseId_;
        if (_phaseName_ == null)
            throw new IllegalArgumentException();
        _phaseName = _phaseName_;
        if (_phaseDescription_ == null)
            throw new IllegalArgumentException();
        _phaseDescription = _phaseDescription_;
        _prePhaseIds = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _prePhaseIds.variableId(5);
        _nextPhaseId = _nextPhaseId_;
        _subPhases = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Game.TaskBase.BSubPhase.class);
        _subPhases.variableId(7);
    }

    public void assign(BTaskPhase other) {
        setTaskId(other.getTaskId());
        setPhaseId(other.getPhaseId());
        setPhaseName(other.getPhaseName());
        setPhaseDescription(other.getPhaseDescription());
        _prePhaseIds.clear();
        _prePhaseIds.addAll(other._prePhaseIds);
        setNextPhaseId(other.getNextPhaseId());
        _subPhases.clear();
        for (var e : other._subPhases)
            _subPhases.add(e.copy());
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

    private static final class Log__taskId extends Zeze.Transaction.Logs.LogLong {
        public Log__taskId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._taskId = value; }
    }

    private static final class Log__phaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__phaseId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseId = value; }
    }

    private static final class Log__phaseName extends Zeze.Transaction.Logs.LogString {
        public Log__phaseName(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseName = value; }
    }

    private static final class Log__phaseDescription extends Zeze.Transaction.Logs.LogString {
        public Log__phaseDescription(BTaskPhase bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._phaseDescription = value; }
    }

    private static final class Log__nextPhaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__nextPhaseId(BTaskPhase bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTaskPhase)getBelong())._nextPhaseId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTaskPhase: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("taskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseId=").append(getPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseName=").append(getPhaseName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseDescription=").append(getPhaseDescription()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("prePhaseIds=[");
        if (!_prePhaseIds.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _prePhaseIds) {
                sb.append(Zeze.Util.Str.indent(level)).append("Item=").append(_item_).append(',').append(System.lineSeparator());
            }
            level -= 4;
            sb.append(Zeze.Util.Str.indent(level));
        }
        sb.append(']').append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("nextPhaseId=").append(getNextPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("subPhases=[");
        if (!_subPhases.isEmpty()) {
            sb.append(System.lineSeparator());
            level += 4;
            for (var _item_ : _subPhases) {
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
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getPhaseName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getPhaseDescription();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _prePhaseIds;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_)
                    _o_.WriteLong(_v_);
            }
        }
        {
            long _x_ = getNextPhaseId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _subPhases;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.LIST);
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
            setTaskId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setPhaseName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPhaseDescription(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            var _x_ = _prePhaseIds;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setNextPhaseId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            var _x_ = _subPhases;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Game.TaskBase.BSubPhase(), _t_));
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
        _prePhaseIds.initRootInfo(root, this);
        _subPhases.initRootInfo(root, this);
    }

    @Override
    protected void resetChildrenRootInfo() {
        _prePhaseIds.resetRootInfo();
        _subPhases.resetRootInfo();
    }

    @Override
    public boolean negativeCheck() {
        if (getTaskId() < 0)
            return true;
        if (getPhaseId() < 0)
            return true;
        for (var _v_ : _prePhaseIds) {
            if (_v_ < 0)
                return true;
        }
        if (getNextPhaseId() < 0)
            return true;
        for (var _v_ : _subPhases) {
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
                case 1: _taskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _phaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _phaseName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 4: _phaseDescription = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 5: _prePhaseIds.followerApply(vlog); break;
                case 6: _nextPhaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _subPhases.followerApply(vlog); break;
            }
        }
    }
}
