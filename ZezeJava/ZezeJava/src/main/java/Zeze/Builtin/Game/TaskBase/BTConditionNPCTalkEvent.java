// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BTConditionNPCTalkEvent extends Zeze.Transaction.Bean implements BTConditionNPCTalkEventReadOnly {
    public static final long TYPEID = -4899454112203602000L;

    private long _taskId;
    private long _phaseId;
    private boolean _finished; // 如果完成了对话，也可以用这个Event发一条，下面的就留空。
    private int _dialogId;
    private int _dialogOption; // 选择了第几个选项

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
    public boolean isFinished() {
        if (!isManaged())
            return _finished;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _finished;
        var log = (Log__finished)txn.getLog(objectId() + 3);
        return log != null ? log.value : _finished;
    }

    public void setFinished(boolean value) {
        if (!isManaged()) {
            _finished = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__finished(this, 3, value));
    }

    @Override
    public int getDialogId() {
        if (!isManaged())
            return _dialogId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _dialogId;
        var log = (Log__dialogId)txn.getLog(objectId() + 4);
        return log != null ? log.value : _dialogId;
    }

    public void setDialogId(int value) {
        if (!isManaged()) {
            _dialogId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__dialogId(this, 4, value));
    }

    @Override
    public int getDialogOption() {
        if (!isManaged())
            return _dialogOption;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _dialogOption;
        var log = (Log__dialogOption)txn.getLog(objectId() + 5);
        return log != null ? log.value : _dialogOption;
    }

    public void setDialogOption(int value) {
        if (!isManaged()) {
            _dialogOption = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__dialogOption(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BTConditionNPCTalkEvent() {
    }

    @SuppressWarnings("deprecation")
    public BTConditionNPCTalkEvent(long _taskId_, long _phaseId_, boolean _finished_, int _dialogId_, int _dialogOption_) {
        _taskId = _taskId_;
        _phaseId = _phaseId_;
        _finished = _finished_;
        _dialogId = _dialogId_;
        _dialogOption = _dialogOption_;
    }

    public void assign(BTConditionNPCTalkEvent other) {
        setTaskId(other.getTaskId());
        setPhaseId(other.getPhaseId());
        setFinished(other.isFinished());
        setDialogId(other.getDialogId());
        setDialogOption(other.getDialogOption());
    }

    @Deprecated
    public void Assign(BTConditionNPCTalkEvent other) {
        assign(other);
    }

    public BTConditionNPCTalkEvent copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTConditionNPCTalkEvent copy() {
        var copy = new BTConditionNPCTalkEvent();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BTConditionNPCTalkEvent Copy() {
        return copy();
    }

    public static void swap(BTConditionNPCTalkEvent a, BTConditionNPCTalkEvent b) {
        BTConditionNPCTalkEvent save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__taskId extends Zeze.Transaction.Logs.LogLong {
        public Log__taskId(BTConditionNPCTalkEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._taskId = value; }
    }

    private static final class Log__phaseId extends Zeze.Transaction.Logs.LogLong {
        public Log__phaseId(BTConditionNPCTalkEvent bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._phaseId = value; }
    }

    private static final class Log__finished extends Zeze.Transaction.Logs.LogBool {
        public Log__finished(BTConditionNPCTalkEvent bean, int varId, boolean value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._finished = value; }
    }

    private static final class Log__dialogId extends Zeze.Transaction.Logs.LogInt {
        public Log__dialogId(BTConditionNPCTalkEvent bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._dialogId = value; }
    }

    private static final class Log__dialogOption extends Zeze.Transaction.Logs.LogInt {
        public Log__dialogOption(BTConditionNPCTalkEvent bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BTConditionNPCTalkEvent)getBelong())._dialogOption = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("taskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("phaseId=").append(getPhaseId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("finished=").append(isFinished()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("dialogId=").append(getDialogId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("dialogOption=").append(getDialogOption()).append(System.lineSeparator());
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
            boolean _x_ = isFinished();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = getDialogId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = getDialogOption();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
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
            setFinished(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setDialogId(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setDialogOption(_o_.ReadInt(_t_));
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
        if (getTaskId() < 0)
            return true;
        if (getPhaseId() < 0)
            return true;
        if (getDialogId() < 0)
            return true;
        if (getDialogOption() < 0)
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
                case 1: _taskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _phaseId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _finished = ((Zeze.Transaction.Logs.LogBool)vlog).value; break;
                case 4: _dialogId = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 5: _dialogOption = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }
}
