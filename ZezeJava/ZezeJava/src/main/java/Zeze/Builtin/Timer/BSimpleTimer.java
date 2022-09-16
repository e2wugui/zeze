// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression"})
public final class BSimpleTimer extends Zeze.Transaction.Bean {
    private long _Delay;
    private long _Period;
    private long _RemainTimes; // -1 表示不限次数。
    private long _HappenTimes;
    private long _StartTimeInMills;
    private long _EndTimeInMills; // -1表示没有结束时间
    private long _NextExpectedTimeMills;
    private long _ExpectedTimeMills;
    private long _HappenTimeMills;

    public long getDelay() {
        if (!isManaged())
            return _Delay;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Delay;
        var log = (Log__Delay)txn.getLog(objectId() + 1);
        return log != null ? log.value : _Delay;
    }

    public void setDelay(long value) {
        if (!isManaged()) {
            _Delay = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Delay(this, 1, value));
    }

    public long getPeriod() {
        if (!isManaged())
            return _Period;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _Period;
        var log = (Log__Period)txn.getLog(objectId() + 2);
        return log != null ? log.value : _Period;
    }

    public void setPeriod(long value) {
        if (!isManaged()) {
            _Period = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__Period(this, 2, value));
    }

    public long getRemainTimes() {
        if (!isManaged())
            return _RemainTimes;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RemainTimes;
        var log = (Log__RemainTimes)txn.getLog(objectId() + 3);
        return log != null ? log.value : _RemainTimes;
    }

    public void setRemainTimes(long value) {
        if (!isManaged()) {
            _RemainTimes = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RemainTimes(this, 3, value));
    }

    public long getHappenTimes() {
        if (!isManaged())
            return _HappenTimes;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HappenTimes;
        var log = (Log__HappenTimes)txn.getLog(objectId() + 4);
        return log != null ? log.value : _HappenTimes;
    }

    public void setHappenTimes(long value) {
        if (!isManaged()) {
            _HappenTimes = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HappenTimes(this, 4, value));
    }

    public long getStartTimeInMills() {
        if (!isManaged())
            return _StartTimeInMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _StartTimeInMills;
        var log = (Log__StartTimeInMills)txn.getLog(objectId() + 5);
        return log != null ? log.value : _StartTimeInMills;
    }

    public void setStartTimeInMills(long value) {
        if (!isManaged()) {
            _StartTimeInMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__StartTimeInMills(this, 5, value));
    }

    public long getEndTimeInMills() {
        if (!isManaged())
            return _EndTimeInMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EndTimeInMills;
        var log = (Log__EndTimeInMills)txn.getLog(objectId() + 6);
        return log != null ? log.value : _EndTimeInMills;
    }

    public void setEndTimeInMills(long value) {
        if (!isManaged()) {
            _EndTimeInMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EndTimeInMills(this, 6, value));
    }

    public long getNextExpectedTimeMills() {
        if (!isManaged())
            return _NextExpectedTimeMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextExpectedTimeMills;
        var log = (Log__NextExpectedTimeMills)txn.getLog(objectId() + 7);
        return log != null ? log.value : _NextExpectedTimeMills;
    }

    public void setNextExpectedTimeMills(long value) {
        if (!isManaged()) {
            _NextExpectedTimeMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextExpectedTimeMills(this, 7, value));
    }

    public long getExpectedTimeMills() {
        if (!isManaged())
            return _ExpectedTimeMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExpectedTimeMills;
        var log = (Log__ExpectedTimeMills)txn.getLog(objectId() + 8);
        return log != null ? log.value : _ExpectedTimeMills;
    }

    public void setExpectedTimeMills(long value) {
        if (!isManaged()) {
            _ExpectedTimeMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExpectedTimeMills(this, 8, value));
    }

    public long getHappenTimeMills() {
        if (!isManaged())
            return _HappenTimeMills;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HappenTimeMills;
        var log = (Log__HappenTimeMills)txn.getLog(objectId() + 9);
        return log != null ? log.value : _HappenTimeMills;
    }

    public void setHappenTimeMills(long value) {
        if (!isManaged()) {
            _HappenTimeMills = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HappenTimeMills(this, 9, value));
    }

    @SuppressWarnings("deprecation")
    public BSimpleTimer() {
    }

    @SuppressWarnings("deprecation")
    public BSimpleTimer(long _Delay_, long _Period_, long _RemainTimes_, long _HappenTimes_, long _StartTimeInMills_, long _EndTimeInMills_, long _NextExpectedTimeMills_, long _ExpectedTimeMills_, long _HappenTimeMills_) {
        _Delay = _Delay_;
        _Period = _Period_;
        _RemainTimes = _RemainTimes_;
        _HappenTimes = _HappenTimes_;
        _StartTimeInMills = _StartTimeInMills_;
        _EndTimeInMills = _EndTimeInMills_;
        _NextExpectedTimeMills = _NextExpectedTimeMills_;
        _ExpectedTimeMills = _ExpectedTimeMills_;
        _HappenTimeMills = _HappenTimeMills_;
    }

    public void assign(BSimpleTimer other) {
        setDelay(other.getDelay());
        setPeriod(other.getPeriod());
        setRemainTimes(other.getRemainTimes());
        setHappenTimes(other.getHappenTimes());
        setStartTimeInMills(other.getStartTimeInMills());
        setEndTimeInMills(other.getEndTimeInMills());
        setNextExpectedTimeMills(other.getNextExpectedTimeMills());
        setExpectedTimeMills(other.getExpectedTimeMills());
        setHappenTimeMills(other.getHappenTimeMills());
    }

    @Deprecated
    public void Assign(BSimpleTimer other) {
        assign(other);
    }

    public BSimpleTimer copyIfManaged() {
        return isManaged() ? Copy() : this;
    }

    public BSimpleTimer copy() {
        var copy = new BSimpleTimer();
        copy.assign(this);
        return copy;
    }

    @Deprecated
    public BSimpleTimer Copy() {
        return copy();
    }

    public static void swap(BSimpleTimer a, BSimpleTimer b) {
        BSimpleTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public BSimpleTimer copyBean() {
        return Copy();
    }

    public static final long TYPEID = 1832177636612857692L;

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__Delay extends Zeze.Transaction.Logs.LogLong {
        public Log__Delay(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._Delay = value; }
    }

    private static final class Log__Period extends Zeze.Transaction.Logs.LogLong {
        public Log__Period(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._Period = value; }
    }

    private static final class Log__RemainTimes extends Zeze.Transaction.Logs.LogLong {
        public Log__RemainTimes(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._RemainTimes = value; }
    }

    private static final class Log__HappenTimes extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTimes(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._HappenTimes = value; }
    }

    private static final class Log__StartTimeInMills extends Zeze.Transaction.Logs.LogLong {
        public Log__StartTimeInMills(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._StartTimeInMills = value; }
    }

    private static final class Log__EndTimeInMills extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTimeInMills(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._EndTimeInMills = value; }
    }

    private static final class Log__NextExpectedTimeMills extends Zeze.Transaction.Logs.LogLong {
        public Log__NextExpectedTimeMills(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._NextExpectedTimeMills = value; }
    }

    private static final class Log__ExpectedTimeMills extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpectedTimeMills(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._ExpectedTimeMills = value; }
    }

    private static final class Log__HappenTimeMills extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTimeMills(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._HappenTimeMills = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BSimpleTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Delay").append('=').append(getDelay()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Period").append('=').append(getPeriod()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RemainTimes").append('=').append(getRemainTimes()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HappenTimes").append('=').append(getHappenTimes()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("StartTimeInMills").append('=').append(getStartTimeInMills()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTimeInMills").append('=').append(getEndTimeInMills()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextExpectedTimeMills").append('=').append(getNextExpectedTimeMills()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExpectedTimeMills").append('=').append(getExpectedTimeMills()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HappenTimeMills").append('=').append(getHappenTimeMills()).append(System.lineSeparator());
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
            long _x_ = getDelay();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getPeriod();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getRemainTimes();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getHappenTimes();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getStartTimeInMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEndTimeInMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getNextExpectedTimeMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getExpectedTimeMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getHappenTimeMills();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
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
            setDelay(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setPeriod(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setRemainTimes(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setHappenTimes(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setStartTimeInMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setEndTimeInMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setNextExpectedTimeMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setExpectedTimeMills(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setHappenTimeMills(_o_.ReadLong(_t_));
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
        if (getDelay() < 0)
            return true;
        if (getPeriod() < 0)
            return true;
        if (getRemainTimes() < 0)
            return true;
        if (getHappenTimes() < 0)
            return true;
        if (getStartTimeInMills() < 0)
            return true;
        if (getEndTimeInMills() < 0)
            return true;
        if (getNextExpectedTimeMills() < 0)
            return true;
        if (getExpectedTimeMills() < 0)
            return true;
        if (getHappenTimeMills() < 0)
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
                case 1: _Delay = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 2: _Period = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _RemainTimes = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _HappenTimes = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _StartTimeInMills = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _EndTimeInMills = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _NextExpectedTimeMills = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 8: _ExpectedTimeMills = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 9: _HappenTimeMills = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
            }
        }
    }
}
