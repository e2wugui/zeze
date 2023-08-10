// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"UnusedAssignment", "RedundantIfStatement", "SwitchStatementWithTooFewBranches", "RedundantSuppression", "NullableProblems", "SuspiciousNameCombination"})
public final class BCronTimer extends Zeze.Transaction.Bean implements BCronTimerReadOnly {
    public static final long TYPEID = -6995089347718168392L;

    private String _CronExpression;
    private long _NextExpectedTime;
    private long _ExpectedTime;
    private long _HappenTime;
    private long _RemainTimes; // -1 表示不限次数。
    private long _EndTime; // 结束时间 -1 表示永不结束
    private int _MissfirePolicy;

    @Override
    public String getCronExpression() {
        if (!isManaged())
            return _CronExpression;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _CronExpression;
        var log = (Log__CronExpression)txn.getLog(objectId() + 1);
        return log != null ? log.value : _CronExpression;
    }

    public void setCronExpression(String value) {
        if (value == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CronExpression = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__CronExpression(this, 1, value));
    }

    @Override
    public long getNextExpectedTime() {
        if (!isManaged())
            return _NextExpectedTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _NextExpectedTime;
        var log = (Log__NextExpectedTime)txn.getLog(objectId() + 2);
        return log != null ? log.value : _NextExpectedTime;
    }

    public void setNextExpectedTime(long value) {
        if (!isManaged()) {
            _NextExpectedTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__NextExpectedTime(this, 2, value));
    }

    @Override
    public long getExpectedTime() {
        if (!isManaged())
            return _ExpectedTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _ExpectedTime;
        var log = (Log__ExpectedTime)txn.getLog(objectId() + 3);
        return log != null ? log.value : _ExpectedTime;
    }

    public void setExpectedTime(long value) {
        if (!isManaged()) {
            _ExpectedTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__ExpectedTime(this, 3, value));
    }

    @Override
    public long getHappenTime() {
        if (!isManaged())
            return _HappenTime;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _HappenTime;
        var log = (Log__HappenTime)txn.getLog(objectId() + 4);
        return log != null ? log.value : _HappenTime;
    }

    public void setHappenTime(long value) {
        if (!isManaged()) {
            _HappenTime = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__HappenTime(this, 4, value));
    }

    @Override
    public long getRemainTimes() {
        if (!isManaged())
            return _RemainTimes;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _RemainTimes;
        var log = (Log__RemainTimes)txn.getLog(objectId() + 5);
        return log != null ? log.value : _RemainTimes;
    }

    public void setRemainTimes(long value) {
        if (!isManaged()) {
            _RemainTimes = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__RemainTimes(this, 5, value));
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
    public int getMissfirePolicy() {
        if (!isManaged())
            return _MissfirePolicy;
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (txn == null)
            return _MissfirePolicy;
        var log = (Log__MissfirePolicy)txn.getLog(objectId() + 7);
        return log != null ? log.value : _MissfirePolicy;
    }

    public void setMissfirePolicy(int value) {
        if (!isManaged()) {
            _MissfirePolicy = value;
            return;
        }
        var txn = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        txn.putLog(new Log__MissfirePolicy(this, 7, value));
    }

    @SuppressWarnings("deprecation")
    public BCronTimer() {
        _CronExpression = "";
    }

    @SuppressWarnings("deprecation")
    public BCronTimer(String _CronExpression_, long _NextExpectedTime_, long _ExpectedTime_, long _HappenTime_, long _RemainTimes_, long _EndTime_, int _MissfirePolicy_) {
        if (_CronExpression_ == null)
            _CronExpression_ = "";
        _CronExpression = _CronExpression_;
        _NextExpectedTime = _NextExpectedTime_;
        _ExpectedTime = _ExpectedTime_;
        _HappenTime = _HappenTime_;
        _RemainTimes = _RemainTimes_;
        _EndTime = _EndTime_;
        _MissfirePolicy = _MissfirePolicy_;
    }

    @Override
    public void reset() {
        setCronExpression("");
        setNextExpectedTime(0);
        setExpectedTime(0);
        setHappenTime(0);
        setRemainTimes(0);
        setEndTime(0);
        setMissfirePolicy(0);
        _unknown_ = null;
    }

    public void assign(BCronTimer other) {
        setCronExpression(other.getCronExpression());
        setNextExpectedTime(other.getNextExpectedTime());
        setExpectedTime(other.getExpectedTime());
        setHappenTime(other.getHappenTime());
        setRemainTimes(other.getRemainTimes());
        setEndTime(other.getEndTime());
        setMissfirePolicy(other.getMissfirePolicy());
        _unknown_ = other._unknown_;
    }

    public BCronTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCronTimer copy() {
        var copy = new BCronTimer();
        copy.assign(this);
        return copy;
    }

    public static void swap(BCronTimer a, BCronTimer b) {
        BCronTimer save = a.copy();
        a.assign(b);
        b.assign(save);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CronExpression extends Zeze.Transaction.Logs.LogString {
        public Log__CronExpression(BCronTimer bean, int varId, String value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._CronExpression = value; }
    }

    private static final class Log__NextExpectedTime extends Zeze.Transaction.Logs.LogLong {
        public Log__NextExpectedTime(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._NextExpectedTime = value; }
    }

    private static final class Log__ExpectedTime extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpectedTime(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._ExpectedTime = value; }
    }

    private static final class Log__HappenTime extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTime(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._HappenTime = value; }
    }

    private static final class Log__RemainTimes extends Zeze.Transaction.Logs.LogLong {
        public Log__RemainTimes(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._RemainTimes = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BCronTimer bean, int varId, long value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._EndTime = value; }
    }

    private static final class Log__MissfirePolicy extends Zeze.Transaction.Logs.LogInt {
        public Log__MissfirePolicy(BCronTimer bean, int varId, int value) { super(bean, varId, value); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._MissfirePolicy = value; }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        return sb.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Timer.BCronTimer: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("CronExpression=").append(getCronExpression()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NextExpectedTime=").append(getNextExpectedTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ExpectedTime=").append(getExpectedTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("HappenTime=").append(getHappenTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("RemainTimes=").append(getRemainTimes()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("EndTime=").append(getEndTime()).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("MissfirePolicy=").append(getMissfirePolicy()).append(System.lineSeparator());
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
            String _x_ = getCronExpression();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getNextExpectedTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getExpectedTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getHappenTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getRemainTimes();
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
            int _x_ = getMissfirePolicy();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setCronExpression(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setNextExpectedTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setExpectedTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setHappenTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setRemainTimes(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setEndTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setMissfirePolicy(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean negativeCheck() {
        if (getNextExpectedTime() < 0)
            return true;
        if (getExpectedTime() < 0)
            return true;
        if (getHappenTime() < 0)
            return true;
        if (getRemainTimes() < 0)
            return true;
        if (getEndTime() < 0)
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
                case 1: _CronExpression = ((Zeze.Transaction.Logs.LogString)vlog).value; break;
                case 2: _NextExpectedTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 3: _ExpectedTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 4: _HappenTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 5: _RemainTimes = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 6: _EndTime = ((Zeze.Transaction.Logs.LogLong)vlog).value; break;
                case 7: _MissfirePolicy = ((Zeze.Transaction.Logs.LogInt)vlog).value; break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        setCronExpression(rs.getString(_parents_name_ + "CronExpression"));
        if (getCronExpression() == null)
            setCronExpression("");
        setNextExpectedTime(rs.getLong(_parents_name_ + "NextExpectedTime"));
        setExpectedTime(rs.getLong(_parents_name_ + "ExpectedTime"));
        setHappenTime(rs.getLong(_parents_name_ + "HappenTime"));
        setRemainTimes(rs.getLong(_parents_name_ + "RemainTimes"));
        setEndTime(rs.getLong(_parents_name_ + "EndTime"));
        setMissfirePolicy(rs.getInt(_parents_name_ + "MissfirePolicy"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "CronExpression", getCronExpression());
        st.appendLong(_parents_name_ + "NextExpectedTime", getNextExpectedTime());
        st.appendLong(_parents_name_ + "ExpectedTime", getExpectedTime());
        st.appendLong(_parents_name_ + "HappenTime", getHappenTime());
        st.appendLong(_parents_name_ + "RemainTimes", getRemainTimes());
        st.appendLong(_parents_name_ + "EndTime", getEndTime());
        st.appendInt(_parents_name_ + "MissfirePolicy", getMissfirePolicy());
    }

    @Override
    public java.util.List<Zeze.Transaction.Bean.Variable> variables() {
        var vars = super.variables();
        vars.add(new Zeze.Transaction.Bean.Variable(1, "CronExpression", "string", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(2, "NextExpectedTime", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(3, "ExpectedTime", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(4, "HappenTime", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(5, "RemainTimes", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(6, "EndTime", "long", "", ""));
        vars.add(new Zeze.Transaction.Bean.Variable(7, "MissfirePolicy", "int", "", ""));
        return vars;
    }

    @Override
    public Zeze.Transaction.Bean toPrevious() {
        return null; // todo BCronTimer
    }
}
