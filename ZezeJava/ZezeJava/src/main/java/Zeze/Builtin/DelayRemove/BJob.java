// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BJob extends Zeze.Transaction.Bean implements BJobReadOnly {
    public static final long TYPEID = -489344497836886892L;

    private String _JobHandleName;
    private Zeze.Net.Binary _JobState;

    @Override
    public String getJobHandleName() {
        if (!isManaged())
            return _JobHandleName;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _JobHandleName;
        var log = (Log__JobHandleName)txn.getLog(objectId() + 1);
        return log != null ? log.value : _JobHandleName;
    }

    public void setJobHandleName(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobHandleName = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__JobHandleName(this, 1, value));
    }

    @Override
    public Zeze.Net.Binary getJobState() {
        if (!isManaged())
            return _JobState;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _JobState;
        var log = (Log__JobState)txn.getLog(objectId() + 2);
        return log != null ? log.value : _JobState;
    }

    public void setJobState(Zeze.Net.Binary value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _JobState = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__JobState(this, 2, value));
    }

    @SuppressWarnings("deprecation")
    public BJob() {
        _JobHandleName = "";
        _JobState = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BJob(String _JobHandleName_, Zeze.Net.Binary _JobState_) {
        if (_JobHandleName_ == null)
            throw new IllegalArgumentException();
        _JobHandleName = _JobHandleName_;
        if (_JobState_ == null)
            throw new IllegalArgumentException();
        _JobState = _JobState_;
    }

    public void assign(BJob other) {
        setJobHandleName(other.getJobHandleName());
        setJobState(other.getJobState());
    }

    @Deprecated
    public void Assign(BJob other) {
        assign(other);
    }

    public BJob copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BJob copy() {
        var copy = new BJob();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BJob Copy() {
        return copy();
    }

    public static void swap(BJob a, BJob b) {
        BJob save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__JobHandleName extends Zeze.Transaction.Logs.LogString {
        public Log__JobHandleName(BJob bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BJob)getBelong())._JobHandleName = value; }
    }

    private static final class Log__JobState extends Zeze.Transaction.Logs.LogBinary {
        public Log__JobState(BJob bean, int varId, Zeze.Net.Binary value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BJob)getBelong())._JobState = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.DelayRemove.BJob: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("JobHandleName=").append(getJobHandleName()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("JobState=").append(getJobState()).append(System.lineSeparator());
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
            String _x_ = getJobHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getJobState();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setJobHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setJobState(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
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
                case 1: _JobHandleName = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _JobState = ((Zeze.Transaction.Logs.LogBinary)vlog).value; break;
            }
        }
    }
}