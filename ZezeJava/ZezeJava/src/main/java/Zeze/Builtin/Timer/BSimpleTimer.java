// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSimpleTimer extends Zeze.Transaction.Bean implements BSimpleTimerReadOnly {
    public static final long TYPEID = 1832177636612857692L;

    private long _Delay;
    private long _Period;
    private long _RemainTimes; // -1 表示不限次数。
    private long _HappenTimes;
    private long _StartTime;
    private long _EndTime; // -1表示没有结束时间
    private long _NextExpectedTime;
    private long _ExpectedTime;
    private long _HappenTime;
    private int _MissfirePolicy;
    private String _OneByOneKey;

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public long getStartTime() {
        if (!isManaged())
            return _StartTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _StartTime;
        var log = (Log__StartTime)txn.getLog(objectId() + 5);
        return log != null ? log.value : _StartTime;
    }

    public void setStartTime(long value) {
        if (!isManaged()) {
            _StartTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__StartTime(this, 5, value));
    }

    @Override
    public long getEndTime() {
        if (!isManaged())
            return _EndTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _EndTime;
        var log = (Log__EndTime)txn.getLog(objectId() + 6);
        return log != null ? log.value : _EndTime;
    }

    public void setEndTime(long value) {
        if (!isManaged()) {
            _EndTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__EndTime(this, 6, value));
    }

    @Override
    public long getNextExpectedTime() {
        if (!isManaged())
            return _NextExpectedTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextExpectedTime;
        var log = (Log__NextExpectedTime)txn.getLog(objectId() + 7);
        return log != null ? log.value : _NextExpectedTime;
    }

    public void setNextExpectedTime(long value) {
        if (!isManaged()) {
            _NextExpectedTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextExpectedTime(this, 7, value));
    }

    @Override
    public long getExpectedTime() {
        if (!isManaged())
            return _ExpectedTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExpectedTime;
        var log = (Log__ExpectedTime)txn.getLog(objectId() + 8);
        return log != null ? log.value : _ExpectedTime;
    }

    public void setExpectedTime(long value) {
        if (!isManaged()) {
            _ExpectedTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExpectedTime(this, 8, value));
    }

    @Override
    public long getHappenTime() {
        if (!isManaged())
            return _HappenTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HappenTime;
        var log = (Log__HappenTime)txn.getLog(objectId() + 9);
        return log != null ? log.value : _HappenTime;
    }

    public void setHappenTime(long value) {
        if (!isManaged()) {
            _HappenTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HappenTime(this, 9, value));
    }

    @Override
    public int getMissfirePolicy() {
        if (!isManaged())
            return _MissfirePolicy;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MissfirePolicy;
        var log = (Log__MissfirePolicy)txn.getLog(objectId() + 10);
        return log != null ? log.value : _MissfirePolicy;
    }

    public void setMissfirePolicy(int value) {
        if (!isManaged()) {
            _MissfirePolicy = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MissfirePolicy(this, 10, value));
    }

    @Override
    public String getOneByOneKey() {
        if (!isManaged())
            return _OneByOneKey;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _OneByOneKey;
        var log = (Log__OneByOneKey)txn.getLog(objectId() + 11);
        return log != null ? log.value : _OneByOneKey;
    }

    public void setOneByOneKey(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OneByOneKey = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__OneByOneKey(this, 11, value));
    }

    @SuppressWarnings("deprecation")
    public BSimpleTimer() {
        _OneByOneKey = "";
    }

    @SuppressWarnings("deprecation")
    public BSimpleTimer(long _Delay_, long _Period_, long _RemainTimes_, long _HappenTimes_, long _StartTime_, long _EndTime_, long _NextExpectedTime_, long _ExpectedTime_, long _HappenTime_, int _MissfirePolicy_, String _OneByOneKey_) {
        _Delay = _Delay_;
        _Period = _Period_;
        _RemainTimes = _RemainTimes_;
        _HappenTimes = _HappenTimes_;
        _StartTime = _StartTime_;
        _EndTime = _EndTime_;
        _NextExpectedTime = _NextExpectedTime_;
        _ExpectedTime = _ExpectedTime_;
        _HappenTime = _HappenTime_;
        _MissfirePolicy = _MissfirePolicy_;
        if (_OneByOneKey_ == null)
            _OneByOneKey_ = "";
        _OneByOneKey = _OneByOneKey_;
    }

    @Override
    public void reset() {
        setDelay(0);
        setPeriod(0);
        setRemainTimes(0);
        setHappenTimes(0);
        setStartTime(0);
        setEndTime(0);
        setNextExpectedTime(0);
        setExpectedTime(0);
        setHappenTime(0);
        setMissfirePolicy(0);
        setOneByOneKey("");
        _unknown_ = null;
    }

    public void assign(BSimpleTimer other) {
        setDelay(other.getDelay());
        setPeriod(other.getPeriod());
        setRemainTimes(other.getRemainTimes());
        setHappenTimes(other.getHappenTimes());
        setStartTime(other.getStartTime());
        setEndTime(other.getEndTime());
        setNextExpectedTime(other.getNextExpectedTime());
        setExpectedTime(other.getExpectedTime());
        setHappenTime(other.getHappenTime());
        setMissfirePolicy(other.getMissfirePolicy());
        setOneByOneKey(other.getOneByOneKey());
        _unknown_ = other._unknown_;
    }

    public BSimpleTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSimpleTimer copy() {
        var copy = new BSimpleTimer();
        copy.assign(this);
        return copy;
    }

    public static void swap(BSimpleTimer a, BSimpleTimer b) {
        BSimpleTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

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

    private static final class Log__StartTime extends Zeze.Transaction.Logs.LogLong {
        public Log__StartTime(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._StartTime = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._EndTime = value; }
    }

    private static final class Log__NextExpectedTime extends Zeze.Transaction.Logs.LogLong {
        public Log__NextExpectedTime(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._NextExpectedTime = value; }
    }

    private static final class Log__ExpectedTime extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpectedTime(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._ExpectedTime = value; }
    }

    private static final class Log__HappenTime extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTime(BSimpleTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._HappenTime = value; }
    }

    private static final class Log__MissfirePolicy extends Zeze.Transaction.Logs.LogInt {
        public Log__MissfirePolicy(BSimpleTimer bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._MissfirePolicy = value; }
    }

    private static final class Log__OneByOneKey extends Zeze.Transaction.Logs.LogString {
        public Log__OneByOneKey(BSimpleTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BSimpleTimer)getBelong())._OneByOneKey = value; }
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
        sb.append(Zeze.Util.Str.indent(level)).append("Delay=").append(getDelay()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Period=").append(getPeriod()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RemainTimes=").append(getRemainTimes()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HappenTimes=").append(getHappenTimes()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("StartTime=").append(getStartTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTime=").append(getEndTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextExpectedTime=").append(getNextExpectedTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExpectedTime=").append(getExpectedTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HappenTime=").append(getHappenTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MissfirePolicy=").append(getMissfirePolicy()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("OneByOneKey=").append(getOneByOneKey()).append(System.lineSeparator());
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
            long _x_ = getStartTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEndTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getNextExpectedTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getExpectedTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getHappenTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = getMissfirePolicy();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getOneByOneKey();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 11, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
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
            setStartTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setEndTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setNextExpectedTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setExpectedTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setHappenTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            setMissfirePolicy(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 11) {
            setOneByOneKey(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSimpleTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSimpleTimer)_o_;
        if (getDelay() != _b_.getDelay())
            return false;
        if (getPeriod() != _b_.getPeriod())
            return false;
        if (getRemainTimes() != _b_.getRemainTimes())
            return false;
        if (getHappenTimes() != _b_.getHappenTimes())
            return false;
        if (getStartTime() != _b_.getStartTime())
            return false;
        if (getEndTime() != _b_.getEndTime())
            return false;
        if (getNextExpectedTime() != _b_.getNextExpectedTime())
            return false;
        if (getExpectedTime() != _b_.getExpectedTime())
            return false;
        if (getHappenTime() != _b_.getHappenTime())
            return false;
        if (getMissfirePolicy() != _b_.getMissfirePolicy())
            return false;
        if (!getOneByOneKey().equals(_b_.getOneByOneKey()))
            return false;
        return true;
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
        if (getStartTime() < 0)
            return true;
        if (getEndTime() < 0)
            return true;
        if (getNextExpectedTime() < 0)
            return true;
        if (getExpectedTime() < 0)
            return true;
        if (getHappenTime() < 0)
            return true;
        if (getMissfirePolicy() < 0)
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
                case 5: _StartTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _EndTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _NextExpectedTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 8: _ExpectedTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 9: _HappenTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 10: _MissfirePolicy = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
                case 11: _OneByOneKey = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setDelay(rs.getLong(_parents_name_ + "Delay"));
        setPeriod(rs.getLong(_parents_name_ + "Period"));
        setRemainTimes(rs.getLong(_parents_name_ + "RemainTimes"));
        setHappenTimes(rs.getLong(_parents_name_ + "HappenTimes"));
        setStartTime(rs.getLong(_parents_name_ + "StartTime"));
        setEndTime(rs.getLong(_parents_name_ + "EndTime"));
        setNextExpectedTime(rs.getLong(_parents_name_ + "NextExpectedTime"));
        setExpectedTime(rs.getLong(_parents_name_ + "ExpectedTime"));
        setHappenTime(rs.getLong(_parents_name_ + "HappenTime"));
        setMissfirePolicy(rs.getInt(_parents_name_ + "MissfirePolicy"));
        setOneByOneKey(rs.getString(_parents_name_ + "OneByOneKey"));
        if (getOneByOneKey() == null)
            setOneByOneKey("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendLong(_parents_name_ + "Delay", getDelay());
        st.appendLong(_parents_name_ + "Period", getPeriod());
        st.appendLong(_parents_name_ + "RemainTimes", getRemainTimes());
        st.appendLong(_parents_name_ + "HappenTimes", getHappenTimes());
        st.appendLong(_parents_name_ + "StartTime", getStartTime());
        st.appendLong(_parents_name_ + "EndTime", getEndTime());
        st.appendLong(_parents_name_ + "NextExpectedTime", getNextExpectedTime());
        st.appendLong(_parents_name_ + "ExpectedTime", getExpectedTime());
        st.appendLong(_parents_name_ + "HappenTime", getHappenTime());
        st.appendInt(_parents_name_ + "MissfirePolicy", getMissfirePolicy());
        st.appendString(_parents_name_ + "OneByOneKey", getOneByOneKey());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Delay", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Period", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "RemainTimes", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "HappenTimes", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "StartTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "EndTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "NextExpectedTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "ExpectedTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "HappenTime", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(10, "MissfirePolicy", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(11, "OneByOneKey", "string", "", ""));
        return vars;
    }
}
