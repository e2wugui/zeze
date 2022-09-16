// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BQueueTask extends Zeze.Transaction.Bean {
    private String _QueueName; // 队列名称。
    private int _TaskType; // 任务类型。
    private long _TaskId; // 任务编号，必须递增。
    private Zeze.Net.Binary _TaskParam; // 任务参数。
    private long _PrevTaskId; // 上一个任务编号，用来发现错误。

    public String getQueueName() {
        if (!isManaged())
            return _QueueName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _QueueName;
        var log = (Log__QueueName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _QueueName;
    }

    public void setQueueName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _QueueName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__QueueName(this, 1, value));
    }

    public int getTaskType() {
        if (!isManaged())
            return _TaskType;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskType;
        var log = (Log__TaskType)txn.getLog(objectId() + 2);
        return log != null ? log.value : _TaskType;
    }

    public void setTaskType(int value) {
        if (!isManaged()) {
            _TaskType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskType(this, 2, value));
    }

    public long getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskId;
        var log = (Log__TaskId)txn.getLog(objectId() + 3);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskId(this, 3, value));
    }

    public Zeze.Net.Binary getTaskParam() {
        if (!isManaged())
            return _TaskParam;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _TaskParam;
        var log = (Log__TaskParam)txn.getLog(objectId() + 4);
        return log != null ? log.value : _TaskParam;
    }

    public void setTaskParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskParam = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__TaskParam(this, 4, value));
    }

    public long getPrevTaskId() {
        if (!isManaged())
            return _PrevTaskId;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _PrevTaskId;
        var log = (Log__PrevTaskId)txn.getLog(objectId() + 5);
        return log != null ? log.value : _PrevTaskId;
    }

    public void setPrevTaskId(long value) {
        if (!isManaged()) {
            _PrevTaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__PrevTaskId(this, 5, value));
    }

    @SuppressWarnings("deprecation")
    public BQueueTask() {
        _QueueName = "";
        _TaskParam = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BQueueTask(String _QueueName_, int _TaskType_, long _TaskId_, Zeze.Net.Binary _TaskParam_, long _PrevTaskId_) {
        if (_QueueName_ == null)
            throw new IllegalArgumentException();
        _QueueName = _QueueName_;
        _TaskType = _TaskType_;
        _TaskId = _TaskId_;
        if (_TaskParam_ == null)
            throw new IllegalArgumentException();
        _TaskParam = _TaskParam_;
        _PrevTaskId = _PrevTaskId_;
    }

    public void assign(BQueueTask other) {
        setQueueName(other.getQueueName());
        setTaskType(other.getTaskType());
        setTaskId(other.getTaskId());
        setTaskParam(other.getTaskParam());
        setPrevTaskId(other.getPrevTaskId());
    }

    @Deprecated
    public void Assign(BQueueTask other) {
        assign(other);
    }

    public BQueueTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    public BQueueTask copy() {
        var copy = new BQueueTask();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BQueueTask Copy() {
        return copy();
    }

    public static void swap(BQueueTask a, BQueueTask b) {
        BQueueTask save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BQueueTask copyBean() {
        return copy();
    }

    public static final long TYPEID = 3220291684741669511L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__QueueName extends Zeze.Transaction.Logs.LogString {
        public Log__QueueName(BQueueTask bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._QueueName = value; }
    }

    private static final class Log__TaskType extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskType(BQueueTask bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._TaskType = value; }
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskId(BQueueTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskParam extends Zeze.Transaction.Logs.LogBinary {
        public Log__TaskParam(BQueueTask bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._TaskParam = value; }
    }

    private static final class Log__PrevTaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__PrevTaskId(BQueueTask bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._PrevTaskId = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.RedoQueue.BQueueTask: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("QueueName").append('=').append(getQueueName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskType").append('=').append(getTaskType()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskId").append('=').append(getTaskId()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TaskParam").append('=').append(getTaskParam()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("PrevTaskId").append('=').append(getPrevTaskId()).append(System.lineSeparator());
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
            String _x_ = getQueueName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _x_ = getTaskType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = getTaskParam();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getPrevTaskId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setQueueName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setTaskType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setTaskId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setTaskParam(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setPrevTaskId(_o_.ReadLong(_t_));
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
        if (getTaskType() < 0)
            return true;
        if (getTaskId() < 0)
            return true;
        if (getPrevTaskId() < 0)
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
                case 1: _QueueName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _TaskType = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 3: _TaskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _TaskParam = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
                case 5: _PrevTaskId = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
