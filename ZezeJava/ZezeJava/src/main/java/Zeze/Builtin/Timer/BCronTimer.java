// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 使用cron表达式触发时间的timer
@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCronTimer extends Zeze.Transaction.Bean implements BCronTimerReadOnly {
    public static final long TYPEID = -6995089347718168392L;

    private String _CronExpression; // timer触发时间的cron表达式
    private long _NextExpectedTime; // 下次计划触发的时间(unix毫秒时间戳)
    private long _ExpectedTime; // 上次应该触发的时间(unix毫秒时间戳), 初始为0
    private long _HappenTime; // 上次实际触发的时间(unix毫秒时间戳), 初始为0
    private long _RemainTimes; // 剩余触发次数, -1表示不限次数
    private long _EndTime; // 限制触发的最后时间(unix毫秒时间戳), 计算下次触发时间发现超过则取消定时器, 只有大于0会限制
    private int _MissfirePolicy; // 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
    private String _OneByOneKey; // timer触发时所用的OneByOne队列key
    private long _HappenTimes; // 已经触发的次数, 触发前自增

    @Override
    public String getCronExpression() {
        if (!isManaged())
            return _CronExpression;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _CronExpression;
        var log = (Log__CronExpression)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _CronExpression;
    }

    public void setCronExpression(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CronExpression = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__CronExpression(this, 1, _v_));
    }

    @Override
    public long getNextExpectedTime() {
        if (!isManaged())
            return _NextExpectedTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextExpectedTime;
        var log = (Log__NextExpectedTime)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _NextExpectedTime;
    }

    public void setNextExpectedTime(long _v_) {
        if (!isManaged()) {
            _NextExpectedTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__NextExpectedTime(this, 2, _v_));
    }

    @Override
    public long getExpectedTime() {
        if (!isManaged())
            return _ExpectedTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ExpectedTime;
        var log = (Log__ExpectedTime)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _ExpectedTime;
    }

    public void setExpectedTime(long _v_) {
        if (!isManaged()) {
            _ExpectedTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ExpectedTime(this, 3, _v_));
    }

    @Override
    public long getHappenTime() {
        if (!isManaged())
            return _HappenTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HappenTime;
        var log = (Log__HappenTime)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _HappenTime;
    }

    public void setHappenTime(long _v_) {
        if (!isManaged()) {
            _HappenTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HappenTime(this, 4, _v_));
    }

    @Override
    public long getRemainTimes() {
        if (!isManaged())
            return _RemainTimes;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RemainTimes;
        var log = (Log__RemainTimes)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _RemainTimes;
    }

    public void setRemainTimes(long _v_) {
        if (!isManaged()) {
            _RemainTimes = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__RemainTimes(this, 5, _v_));
    }

    @Override
    public long getEndTime() {
        if (!isManaged())
            return _EndTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EndTime;
        var log = (Log__EndTime)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _EndTime;
    }

    public void setEndTime(long _v_) {
        if (!isManaged()) {
            _EndTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__EndTime(this, 6, _v_));
    }

    @Override
    public int getMissfirePolicy() {
        if (!isManaged())
            return _MissfirePolicy;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MissfirePolicy;
        var log = (Log__MissfirePolicy)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _MissfirePolicy;
    }

    public void setMissfirePolicy(int _v_) {
        if (!isManaged()) {
            _MissfirePolicy = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__MissfirePolicy(this, 7, _v_));
    }

    @Override
    public String getOneByOneKey() {
        if (!isManaged())
            return _OneByOneKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OneByOneKey;
        var log = (Log__OneByOneKey)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _OneByOneKey;
    }

    public void setOneByOneKey(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OneByOneKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__OneByOneKey(this, 8, _v_));
    }

    @Override
    public long getHappenTimes() {
        if (!isManaged())
            return _HappenTimes;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HappenTimes;
        var log = (Log__HappenTimes)_t_.getLog(objectId() + 9);
        return log != null ? log.value : _HappenTimes;
    }

    public void setHappenTimes(long _v_) {
        if (!isManaged()) {
            _HappenTimes = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HappenTimes(this, 9, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCronTimer() {
        _CronExpression = "";
        _OneByOneKey = "";
    }

    @SuppressWarnings("deprecation")
    public BCronTimer(String _CronExpression_, long _NextExpectedTime_, long _ExpectedTime_, long _HappenTime_, long _RemainTimes_, long _EndTime_, int _MissfirePolicy_, String _OneByOneKey_, long _HappenTimes_) {
        if (_CronExpression_ == null)
            _CronExpression_ = "";
        _CronExpression = _CronExpression_;
        _NextExpectedTime = _NextExpectedTime_;
        _ExpectedTime = _ExpectedTime_;
        _HappenTime = _HappenTime_;
        _RemainTimes = _RemainTimes_;
        _EndTime = _EndTime_;
        _MissfirePolicy = _MissfirePolicy_;
        if (_OneByOneKey_ == null)
            _OneByOneKey_ = "";
        _OneByOneKey = _OneByOneKey_;
        _HappenTimes = _HappenTimes_;
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
        setOneByOneKey("");
        setHappenTimes(0);
        _unknown_ = null;
    }

    public void assign(BCronTimer _o_) {
        setCronExpression(_o_.getCronExpression());
        setNextExpectedTime(_o_.getNextExpectedTime());
        setExpectedTime(_o_.getExpectedTime());
        setHappenTime(_o_.getHappenTime());
        setRemainTimes(_o_.getRemainTimes());
        setEndTime(_o_.getEndTime());
        setMissfirePolicy(_o_.getMissfirePolicy());
        setOneByOneKey(_o_.getOneByOneKey());
        setHappenTimes(_o_.getHappenTimes());
        _unknown_ = _o_._unknown_;
    }

    public BCronTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCronTimer copy() {
        var _c_ = new BCronTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCronTimer _a_, BCronTimer _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__CronExpression extends Zeze.Transaction.Logs.LogString {
        public Log__CronExpression(BCronTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._CronExpression = value; }
    }

    private static final class Log__NextExpectedTime extends Zeze.Transaction.Logs.LogLong {
        public Log__NextExpectedTime(BCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._NextExpectedTime = value; }
    }

    private static final class Log__ExpectedTime extends Zeze.Transaction.Logs.LogLong {
        public Log__ExpectedTime(BCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._ExpectedTime = value; }
    }

    private static final class Log__HappenTime extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTime(BCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._HappenTime = value; }
    }

    private static final class Log__RemainTimes extends Zeze.Transaction.Logs.LogLong {
        public Log__RemainTimes(BCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._RemainTimes = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._EndTime = value; }
    }

    private static final class Log__MissfirePolicy extends Zeze.Transaction.Logs.LogInt {
        public Log__MissfirePolicy(BCronTimer _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._MissfirePolicy = value; }
    }

    private static final class Log__OneByOneKey extends Zeze.Transaction.Logs.LogString {
        public Log__OneByOneKey(BCronTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._OneByOneKey = value; }
    }

    private static final class Log__HappenTimes extends Zeze.Transaction.Logs.LogLong {
        public Log__HappenTimes(BCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCronTimer)getBelong())._HappenTimes = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Timer.BCronTimer: {\n");
        _s_.append(_i1_).append("CronExpression=").append(getCronExpression()).append(",\n");
        _s_.append(_i1_).append("NextExpectedTime=").append(getNextExpectedTime()).append(",\n");
        _s_.append(_i1_).append("ExpectedTime=").append(getExpectedTime()).append(",\n");
        _s_.append(_i1_).append("HappenTime=").append(getHappenTime()).append(",\n");
        _s_.append(_i1_).append("RemainTimes=").append(getRemainTimes()).append(",\n");
        _s_.append(_i1_).append("EndTime=").append(getEndTime()).append(",\n");
        _s_.append(_i1_).append("MissfirePolicy=").append(getMissfirePolicy()).append(",\n");
        _s_.append(_i1_).append("OneByOneKey=").append(getOneByOneKey()).append(",\n");
        _s_.append(_i1_).append("HappenTimes=").append(getHappenTimes()).append('\n');
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
        {
            String _x_ = getOneByOneKey();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getHappenTimes();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.INTEGER);
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
        if (_i_ == 8) {
            setOneByOneKey(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            setHappenTimes(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCronTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCronTimer)_o_;
        if (!getCronExpression().equals(_b_.getCronExpression()))
            return false;
        if (getNextExpectedTime() != _b_.getNextExpectedTime())
            return false;
        if (getExpectedTime() != _b_.getExpectedTime())
            return false;
        if (getHappenTime() != _b_.getHappenTime())
            return false;
        if (getRemainTimes() != _b_.getRemainTimes())
            return false;
        if (getEndTime() != _b_.getEndTime())
            return false;
        if (getMissfirePolicy() != _b_.getMissfirePolicy())
            return false;
        if (!getOneByOneKey().equals(_b_.getOneByOneKey()))
            return false;
        if (getHappenTimes() != _b_.getHappenTimes())
            return false;
        return true;
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
        if (getHappenTimes() < 0)
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
                case 1: _CronExpression = _v_.stringValue(); break;
                case 2: _NextExpectedTime = _v_.longValue(); break;
                case 3: _ExpectedTime = _v_.longValue(); break;
                case 4: _HappenTime = _v_.longValue(); break;
                case 5: _RemainTimes = _v_.longValue(); break;
                case 6: _EndTime = _v_.longValue(); break;
                case 7: _MissfirePolicy = _v_.intValue(); break;
                case 8: _OneByOneKey = _v_.stringValue(); break;
                case 9: _HappenTimes = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setCronExpression(_r_.getString(_pn_ + "CronExpression"));
        if (getCronExpression() == null)
            setCronExpression("");
        setNextExpectedTime(_r_.getLong(_pn_ + "NextExpectedTime"));
        setExpectedTime(_r_.getLong(_pn_ + "ExpectedTime"));
        setHappenTime(_r_.getLong(_pn_ + "HappenTime"));
        setRemainTimes(_r_.getLong(_pn_ + "RemainTimes"));
        setEndTime(_r_.getLong(_pn_ + "EndTime"));
        setMissfirePolicy(_r_.getInt(_pn_ + "MissfirePolicy"));
        setOneByOneKey(_r_.getString(_pn_ + "OneByOneKey"));
        if (getOneByOneKey() == null)
            setOneByOneKey("");
        setHappenTimes(_r_.getLong(_pn_ + "HappenTimes"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "CronExpression", getCronExpression());
        _s_.appendLong(_pn_ + "NextExpectedTime", getNextExpectedTime());
        _s_.appendLong(_pn_ + "ExpectedTime", getExpectedTime());
        _s_.appendLong(_pn_ + "HappenTime", getHappenTime());
        _s_.appendLong(_pn_ + "RemainTimes", getRemainTimes());
        _s_.appendLong(_pn_ + "EndTime", getEndTime());
        _s_.appendInt(_pn_ + "MissfirePolicy", getMissfirePolicy());
        _s_.appendString(_pn_ + "OneByOneKey", getOneByOneKey());
        _s_.appendLong(_pn_ + "HappenTimes", getHappenTimes());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "CronExpression", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "NextExpectedTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "ExpectedTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "HappenTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "RemainTimes", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "EndTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "MissfirePolicy", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "OneByOneKey", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "HappenTimes", "long", "", ""));
        return _v_;
    }
}
