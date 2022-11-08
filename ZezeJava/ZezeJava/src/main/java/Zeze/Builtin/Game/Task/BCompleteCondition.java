// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;

// Task rpc
@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BCompleteCondition extends Zeze.Transaction.Bean implements BCompleteConditionReadOnly {
    public static final long TYPEID = 5314708440903539225L;

    private String _TaskId;
    private String _TaskPhaseId;
    private String _TaskConditionId;

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
    public String getTaskPhaseId() {
        if (!isManaged())
            return _TaskPhaseId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskPhaseId;
        var log = (Log__TaskPhaseId)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TaskPhaseId;
    }

    public void setTaskPhaseId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskPhaseId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskPhaseId(this, 2, value));
    }

    @Override
    public String getTaskConditionId() {
        if (!isManaged())
            return _TaskConditionId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskConditionId;
        var log = (Log__TaskConditionId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _TaskConditionId;
    }

    public void setTaskConditionId(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskConditionId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskConditionId(this, 3, value));
    }

    @SuppressWarnings("deprecation")
    public BCompleteCondition() {
        _TaskId = "";
        _TaskPhaseId = "";
        _TaskConditionId = "";
    }

    @SuppressWarnings("deprecation")
    public BCompleteCondition(String _TaskId_, String _TaskPhaseId_, String _TaskConditionId_) {
        if (_TaskId_ == null)
            throw new IllegalArgumentException();
        _TaskId = _TaskId_;
        if (_TaskPhaseId_ == null)
            throw new IllegalArgumentException();
        _TaskPhaseId = _TaskPhaseId_;
        if (_TaskConditionId_ == null)
            throw new IllegalArgumentException();
        _TaskConditionId = _TaskConditionId_;
    }

    public void assign(BCompleteCondition other) {
        setTaskId(other.getTaskId());
        setTaskPhaseId(other.getTaskPhaseId());
        setTaskConditionId(other.getTaskConditionId());
    }

    @Deprecated
    public void Assign(BCompleteCondition other) {
        assign(other);
    }

    public BCompleteCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCompleteCondition copy() {
        var copy = new BCompleteCondition();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BCompleteCondition Copy() {
        return copy();
    }

    public static void swap(BCompleteCondition a, BCompleteCondition b) {
        BCompleteCondition save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogString {
        public Log__TaskId(BCompleteCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCompleteCondition)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskPhaseId extends Zeze.Transaction.Logs.LogString {
        public Log__TaskPhaseId(BCompleteCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCompleteCondition)getBelong())._TaskPhaseId = value; }
    }

    private static final class Log__TaskConditionId extends Zeze.Transaction.Logs.LogString {
        public Log__TaskConditionId(BCompleteCondition bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCompleteCondition)getBelong())._TaskConditionId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Task.BCompleteCondition: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId").append('=').append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskPhaseId").append('=').append(getTaskPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskConditionId").append('=').append(getTaskConditionId()).append(System.lineSeparator());
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
            String _x_ = getTaskPhaseId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getTaskConditionId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setTaskPhaseId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTaskConditionId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    protected void resetChildrenRootInfo() {
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
                case 2: _TaskPhaseId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 3: _TaskConditionId = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }
}
