// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BQueueTask extends Zeze.Transaction.Bean implements BQueueTaskReadOnly {
    public static final long TYPEID = 3220291684741669511L;

    private String _QueueName; // 队列名称。
    private int _TaskType; // 任务类型。
    private long _TaskId; // 任务编号，必须递增。
    private Zeze.Net.Binary _TaskParam; // 任务参数。
    private long _PrevTaskId; // 上一个任务编号，用来发现错误。

    @Override
    public String getQueueName() {
        if (!isManaged())
            return _QueueName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _QueueName;
        var log = (Log__QueueName)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _QueueName;
    }

    public void setQueueName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _QueueName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__QueueName(this, 1, _v_));
    }

    @Override
    public int getTaskType() {
        if (!isManaged())
            return _TaskType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TaskType;
        var log = (Log__TaskType)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _TaskType;
    }

    public void setTaskType(int _v_) {
        if (!isManaged()) {
            _TaskType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TaskType(this, 2, _v_));
    }

    @Override
    public long getTaskId() {
        if (!isManaged())
            return _TaskId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TaskId;
        var log = (Log__TaskId)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _TaskId;
    }

    public void setTaskId(long _v_) {
        if (!isManaged()) {
            _TaskId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TaskId(this, 3, _v_));
    }

    @Override
    public Zeze.Net.Binary getTaskParam() {
        if (!isManaged())
            return _TaskParam;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TaskParam;
        var log = (Log__TaskParam)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _TaskParam;
    }

    public void setTaskParam(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TaskParam = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TaskParam(this, 4, _v_));
    }

    @Override
    public long getPrevTaskId() {
        if (!isManaged())
            return _PrevTaskId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _PrevTaskId;
        var log = (Log__PrevTaskId)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _PrevTaskId;
    }

    public void setPrevTaskId(long _v_) {
        if (!isManaged()) {
            _PrevTaskId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__PrevTaskId(this, 5, _v_));
    }

    @SuppressWarnings("deprecation")
    public BQueueTask() {
        _QueueName = "";
        _TaskParam = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BQueueTask(String _QueueName_, int _TaskType_, long _TaskId_, Zeze.Net.Binary _TaskParam_, long _PrevTaskId_) {
        if (_QueueName_ == null)
            _QueueName_ = "";
        _QueueName = _QueueName_;
        _TaskType = _TaskType_;
        _TaskId = _TaskId_;
        if (_TaskParam_ == null)
            _TaskParam_ = Zeze.Net.Binary.Empty;
        _TaskParam = _TaskParam_;
        _PrevTaskId = _PrevTaskId_;
    }

    @Override
    public void reset() {
        setQueueName("");
        setTaskType(0);
        setTaskId(0);
        setTaskParam(Zeze.Net.Binary.Empty);
        setPrevTaskId(0);
        _unknown_ = null;
    }

    public void assign(BQueueTask _o_) {
        setQueueName(_o_.getQueueName());
        setTaskType(_o_.getTaskType());
        setTaskId(_o_.getTaskId());
        setTaskParam(_o_.getTaskParam());
        setPrevTaskId(_o_.getPrevTaskId());
        _unknown_ = _o_._unknown_;
    }

    public BQueueTask copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BQueueTask copy() {
        var _c_ = new BQueueTask();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BQueueTask _a_, BQueueTask _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__QueueName extends Zeze.Transaction.Logs.LogString {
        public Log__QueueName(BQueueTask _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._QueueName = value; }
    }

    private static final class Log__TaskType extends Zeze.Transaction.Logs.LogInt {
        public Log__TaskType(BQueueTask _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._TaskType = value; }
    }

    private static final class Log__TaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__TaskId(BQueueTask _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._TaskId = value; }
    }

    private static final class Log__TaskParam extends Zeze.Transaction.Logs.LogBinary {
        public Log__TaskParam(BQueueTask _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._TaskParam = value; }
    }

    private static final class Log__PrevTaskId extends Zeze.Transaction.Logs.LogLong {
        public Log__PrevTaskId(BQueueTask _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BQueueTask)getBelong())._PrevTaskId = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.RedoQueue.BQueueTask: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("QueueName=").append(getQueueName()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TaskType=").append(getTaskType()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TaskId=").append(getTaskId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TaskParam=").append(getTaskParam()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("PrevTaskId=").append(getPrevTaskId()).append(System.lineSeparator());
        _l_ -= 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
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
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BQueueTask))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BQueueTask)_o_;
        if (!getQueueName().equals(_b_.getQueueName()))
            return false;
        if (getTaskType() != _b_.getTaskType())
            return false;
        if (getTaskId() != _b_.getTaskId())
            return false;
        if (!getTaskParam().equals(_b_.getTaskParam()))
            return false;
        if (getPrevTaskId() != _b_.getPrevTaskId())
            return false;
        return true;
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _QueueName = _v_.stringValue(); break;
                case 2: _TaskType = _v_.intValue(); break;
                case 3: _TaskId = _v_.longValue(); break;
                case 4: _TaskParam = _v_.binaryValue(); break;
                case 5: _PrevTaskId = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setQueueName(_r_.getString(_pn_ + "QueueName"));
        if (getQueueName() == null)
            setQueueName("");
        setTaskType(_r_.getInt(_pn_ + "TaskType"));
        setTaskId(_r_.getLong(_pn_ + "TaskId"));
        setTaskParam(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "TaskParam")));
        setPrevTaskId(_r_.getLong(_pn_ + "PrevTaskId"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "QueueName", getQueueName());
        _s_.appendInt(_pn_ + "TaskType", getTaskType());
        _s_.appendLong(_pn_ + "TaskId", getTaskId());
        _s_.appendBinary(_pn_ + "TaskParam", getTaskParam());
        _s_.appendLong(_pn_ + "PrevTaskId", getPrevTaskId());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "QueueName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "TaskType", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TaskId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "TaskParam", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "PrevTaskId", "long", "", ""));
        return _v_;
    }
}
