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
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _QueueName;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__QueueName)txn.GetLog(this.getObjectId() + 1);
        return log != null ? log.getValue() : _QueueName;
    }

    public void setQueueName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _QueueName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__QueueName(this, value));
    }

    public int getTaskType() {
        if (!isManaged())
            return _TaskType;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _TaskType;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__TaskType)txn.GetLog(this.getObjectId() + 2);
        return log != null ? log.getValue() : _TaskType;
    }

    public void setTaskType(int value) {
        if (!isManaged()) {
            _TaskType = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__TaskType(this, value));
    }

    public long getTaskId() {
        if (!isManaged())
            return _TaskId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _TaskId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__TaskId)txn.GetLog(this.getObjectId() + 3);
        return log != null ? log.getValue() : _TaskId;
    }

    public void setTaskId(long value) {
        if (!isManaged()) {
            _TaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__TaskId(this, value));
    }

    public Zeze.Net.Binary getTaskParam() {
        if (!isManaged())
            return _TaskParam;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _TaskParam;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__TaskParam)txn.GetLog(this.getObjectId() + 4);
        return log != null ? log.getValue() : _TaskParam;
    }

    public void setTaskParam(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskParam = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__TaskParam(this, value));
    }

    public long getPrevTaskId() {
        if (!isManaged())
            return _PrevTaskId;
        var txn = Zeze.Transaction.Transaction.getCurrent();
        if (txn == null)
            return _PrevTaskId;
        txn.VerifyRecordAccessed(this, true);
        var log = (Log__PrevTaskId)txn.GetLog(this.getObjectId() + 5);
        return log != null ? log.getValue() : _PrevTaskId;
    }

    public void setPrevTaskId(long value) {
        if (!isManaged()) {
            _PrevTaskId = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrent();
        assert txn != null;
        txn.VerifyRecordAccessed(this);
        txn.PutLog(new Log__PrevTaskId(this, value));
    }

    public BQueueTask() {
         this(0);
    }

    public BQueueTask(int _varId_) {
        super(_varId_);
        _QueueName = "";
        _TaskParam = Zeze.Net.Binary.Empty;
    }

    public void Assign(BQueueTask other) {
        setQueueName(other.getQueueName());
        setTaskType(other.getTaskType());
        setTaskId(other.getTaskId());
        setTaskParam(other.getTaskParam());
        setPrevTaskId(other.getPrevTaskId());
    }

    public BQueueTask CopyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BQueueTask Copy() {
        var copy = new BQueueTask();
        copy.Assign(this);
        return copy;
    }

    public static void Swap(BQueueTask a, BQueueTask b) {
        BQueueTask save = a.Copy();
        a.Assign(b);
        b.Assign(save);
    }

    @Override
    public Zeze.Transaction.Bean CopyBean() {
        return Copy();
    }

    public static final long TYPEID = 3220291684741669511L;

    @Override
    public long getTypeId() {
        return TYPEID;
    }

    private static final class Log__QueueName extends Zeze.Transaction.Log1<BQueueTask, String> {
        public Log__QueueName(BQueueTask self, String value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 1; }
        @Override
        public void Commit() { this.getBeanTyped()._QueueName = this.getValue(); }
    }

    private static final class Log__TaskType extends Zeze.Transaction.Log1<BQueueTask, Integer> {
        public Log__TaskType(BQueueTask self, Integer value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 2; }
        @Override
        public void Commit() { this.getBeanTyped()._TaskType = this.getValue(); }
    }

    private static final class Log__TaskId extends Zeze.Transaction.Log1<BQueueTask, Long> {
        public Log__TaskId(BQueueTask self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 3; }
        @Override
        public void Commit() { this.getBeanTyped()._TaskId = this.getValue(); }
    }

    private static final class Log__TaskParam extends Zeze.Transaction.Log1<BQueueTask, Zeze.Net.Binary> {
        public Log__TaskParam(BQueueTask self, Zeze.Net.Binary value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 4; }
        @Override
        public void Commit() { this.getBeanTyped()._TaskParam = this.getValue(); }
    }

    private static final class Log__PrevTaskId extends Zeze.Transaction.Log1<BQueueTask, Long> {
        public Log__PrevTaskId(BQueueTask self, Long value) { super(self, value); }
        @Override
        public long getLogKey() { return this.getBean().getObjectId() + 5; }
        @Override
        public void Commit() { this.getBeanTyped()._PrevTaskId = this.getValue(); }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public void BuildString(StringBuilder sb, int level) {
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
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void Encode(ByteBuffer _o_) {
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
    public void Decode(ByteBuffer _o_) {
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
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
    }

    @Override
    public boolean NegativeCheck() {
        if (getTaskType() < 0)
            return true;
        if (getTaskId() < 0)
            return true;
        if (getPrevTaskId() < 0)
            return true;
        return false;
    }
}
