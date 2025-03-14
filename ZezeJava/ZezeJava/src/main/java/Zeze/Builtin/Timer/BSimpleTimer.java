// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// 固定周期触发的timer
@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSimpleTimer extends Zeze.Transaction.Bean implements BSimpleTimerReadOnly {
    public static final long TYPEID = 1832177636612857692L;

    private long _Delay; // [已废弃]
    private long _Period; // 触发周期(毫秒), 只有大于0才会周期触发
    private long _RemainTimes; // 剩余触发次数, -1表示不限次数
    private long _HappenTimes; // 已经触发的次数, 触发前自增
    private long _StartTime; // timer的创建时间(unix毫秒时间戳)
    private long _EndTime; // 限制触发的最后时间(unix毫秒时间戳), 计算下次触发时间发现超过则取消定时器, 只有大于0会限制
    private long _NextExpectedTime; // 下次计划触发的时间(unix毫秒时间戳)
    private long _ExpectedTime; // 上次应该触发的时间(unix毫秒时间戳), 初始为0
    private long _HappenTime; // 上次实际触发的时间(unix毫秒时间戳), 初始为0
    private int _MissfirePolicy; // 错过指定触发时间的处理方式, 见Timer模块定义的eMissfirePolicy开头枚举
    private String _OneByOneKey; // timer触发时所用的OneByOne队列key

    private static final java.lang.invoke.VarHandle vh_Delay;
    private static final java.lang.invoke.VarHandle vh_Period;
    private static final java.lang.invoke.VarHandle vh_RemainTimes;
    private static final java.lang.invoke.VarHandle vh_HappenTimes;
    private static final java.lang.invoke.VarHandle vh_StartTime;
    private static final java.lang.invoke.VarHandle vh_EndTime;
    private static final java.lang.invoke.VarHandle vh_NextExpectedTime;
    private static final java.lang.invoke.VarHandle vh_ExpectedTime;
    private static final java.lang.invoke.VarHandle vh_HappenTime;
    private static final java.lang.invoke.VarHandle vh_MissfirePolicy;
    private static final java.lang.invoke.VarHandle vh_OneByOneKey;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Delay = _l_.findVarHandle(BSimpleTimer.class, "_Delay", long.class);
            vh_Period = _l_.findVarHandle(BSimpleTimer.class, "_Period", long.class);
            vh_RemainTimes = _l_.findVarHandle(BSimpleTimer.class, "_RemainTimes", long.class);
            vh_HappenTimes = _l_.findVarHandle(BSimpleTimer.class, "_HappenTimes", long.class);
            vh_StartTime = _l_.findVarHandle(BSimpleTimer.class, "_StartTime", long.class);
            vh_EndTime = _l_.findVarHandle(BSimpleTimer.class, "_EndTime", long.class);
            vh_NextExpectedTime = _l_.findVarHandle(BSimpleTimer.class, "_NextExpectedTime", long.class);
            vh_ExpectedTime = _l_.findVarHandle(BSimpleTimer.class, "_ExpectedTime", long.class);
            vh_HappenTime = _l_.findVarHandle(BSimpleTimer.class, "_HappenTime", long.class);
            vh_MissfirePolicy = _l_.findVarHandle(BSimpleTimer.class, "_MissfirePolicy", int.class);
            vh_OneByOneKey = _l_.findVarHandle(BSimpleTimer.class, "_OneByOneKey", String.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getDelay() {
        if (!isManaged())
            return _Delay;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Delay;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Delay;
    }

    public void setDelay(long _v_) {
        if (!isManaged()) {
            _Delay = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Delay, _v_));
    }

    @Override
    public long getPeriod() {
        if (!isManaged())
            return _Period;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Period;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Period;
    }

    public void setPeriod(long _v_) {
        if (!isManaged()) {
            _Period = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_Period, _v_));
    }

    @Override
    public long getRemainTimes() {
        if (!isManaged())
            return _RemainTimes;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RemainTimes;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _RemainTimes;
    }

    public void setRemainTimes(long _v_) {
        if (!isManaged()) {
            _RemainTimes = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_RemainTimes, _v_));
    }

    @Override
    public long getHappenTimes() {
        if (!isManaged())
            return _HappenTimes;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HappenTimes;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _HappenTimes;
    }

    public void setHappenTimes(long _v_) {
        if (!isManaged()) {
            _HappenTimes = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_HappenTimes, _v_));
    }

    @Override
    public long getStartTime() {
        if (!isManaged())
            return _StartTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _StartTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _StartTime;
    }

    public void setStartTime(long _v_) {
        if (!isManaged()) {
            _StartTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_StartTime, _v_));
    }

    @Override
    public long getEndTime() {
        if (!isManaged())
            return _EndTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EndTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _EndTime;
    }

    public void setEndTime(long _v_) {
        if (!isManaged()) {
            _EndTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 6, vh_EndTime, _v_));
    }

    @Override
    public long getNextExpectedTime() {
        if (!isManaged())
            return _NextExpectedTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _NextExpectedTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _NextExpectedTime;
    }

    public void setNextExpectedTime(long _v_) {
        if (!isManaged()) {
            _NextExpectedTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 7, vh_NextExpectedTime, _v_));
    }

    @Override
    public long getExpectedTime() {
        if (!isManaged())
            return _ExpectedTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ExpectedTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _ExpectedTime;
    }

    public void setExpectedTime(long _v_) {
        if (!isManaged()) {
            _ExpectedTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 8, vh_ExpectedTime, _v_));
    }

    @Override
    public long getHappenTime() {
        if (!isManaged())
            return _HappenTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HappenTime;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 9);
        return log != null ? log.value : _HappenTime;
    }

    public void setHappenTime(long _v_) {
        if (!isManaged()) {
            _HappenTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 9, vh_HappenTime, _v_));
    }

    @Override
    public int getMissfirePolicy() {
        if (!isManaged())
            return _MissfirePolicy;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _MissfirePolicy;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 10);
        return log != null ? log.value : _MissfirePolicy;
    }

    public void setMissfirePolicy(int _v_) {
        if (!isManaged()) {
            _MissfirePolicy = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 10, vh_MissfirePolicy, _v_));
    }

    @Override
    public String getOneByOneKey() {
        if (!isManaged())
            return _OneByOneKey;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OneByOneKey;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 11);
        return log != null ? log.stringValue() : _OneByOneKey;
    }

    public void setOneByOneKey(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _OneByOneKey = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 11, vh_OneByOneKey, _v_));
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

    public void assign(BSimpleTimer _o_) {
        setDelay(_o_.getDelay());
        setPeriod(_o_.getPeriod());
        setRemainTimes(_o_.getRemainTimes());
        setHappenTimes(_o_.getHappenTimes());
        setStartTime(_o_.getStartTime());
        setEndTime(_o_.getEndTime());
        setNextExpectedTime(_o_.getNextExpectedTime());
        setExpectedTime(_o_.getExpectedTime());
        setHappenTime(_o_.getHappenTime());
        setMissfirePolicy(_o_.getMissfirePolicy());
        setOneByOneKey(_o_.getOneByOneKey());
        _unknown_ = _o_._unknown_;
    }

    public BSimpleTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSimpleTimer copy() {
        var _c_ = new BSimpleTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSimpleTimer _a_, BSimpleTimer _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
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
        _s_.append("Zeze.Builtin.Timer.BSimpleTimer: {\n");
        _s_.append(_i1_).append("Delay=").append(getDelay()).append(",\n");
        _s_.append(_i1_).append("Period=").append(getPeriod()).append(",\n");
        _s_.append(_i1_).append("RemainTimes=").append(getRemainTimes()).append(",\n");
        _s_.append(_i1_).append("HappenTimes=").append(getHappenTimes()).append(",\n");
        _s_.append(_i1_).append("StartTime=").append(getStartTime()).append(",\n");
        _s_.append(_i1_).append("EndTime=").append(getEndTime()).append(",\n");
        _s_.append(_i1_).append("NextExpectedTime=").append(getNextExpectedTime()).append(",\n");
        _s_.append(_i1_).append("ExpectedTime=").append(getExpectedTime()).append(",\n");
        _s_.append(_i1_).append("HappenTime=").append(getHappenTime()).append(",\n");
        _s_.append(_i1_).append("MissfirePolicy=").append(getMissfirePolicy()).append(",\n");
        _s_.append(_i1_).append("OneByOneKey=").append(getOneByOneKey()).append('\n');
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
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Delay = _v_.longValue(); break;
                case 2: _Period = _v_.longValue(); break;
                case 3: _RemainTimes = _v_.longValue(); break;
                case 4: _HappenTimes = _v_.longValue(); break;
                case 5: _StartTime = _v_.longValue(); break;
                case 6: _EndTime = _v_.longValue(); break;
                case 7: _NextExpectedTime = _v_.longValue(); break;
                case 8: _ExpectedTime = _v_.longValue(); break;
                case 9: _HappenTime = _v_.longValue(); break;
                case 10: _MissfirePolicy = _v_.intValue(); break;
                case 11: _OneByOneKey = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDelay(_r_.getLong(_pn_ + "Delay"));
        setPeriod(_r_.getLong(_pn_ + "Period"));
        setRemainTimes(_r_.getLong(_pn_ + "RemainTimes"));
        setHappenTimes(_r_.getLong(_pn_ + "HappenTimes"));
        setStartTime(_r_.getLong(_pn_ + "StartTime"));
        setEndTime(_r_.getLong(_pn_ + "EndTime"));
        setNextExpectedTime(_r_.getLong(_pn_ + "NextExpectedTime"));
        setExpectedTime(_r_.getLong(_pn_ + "ExpectedTime"));
        setHappenTime(_r_.getLong(_pn_ + "HappenTime"));
        setMissfirePolicy(_r_.getInt(_pn_ + "MissfirePolicy"));
        setOneByOneKey(_r_.getString(_pn_ + "OneByOneKey"));
        if (getOneByOneKey() == null)
            setOneByOneKey("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Delay", getDelay());
        _s_.appendLong(_pn_ + "Period", getPeriod());
        _s_.appendLong(_pn_ + "RemainTimes", getRemainTimes());
        _s_.appendLong(_pn_ + "HappenTimes", getHappenTimes());
        _s_.appendLong(_pn_ + "StartTime", getStartTime());
        _s_.appendLong(_pn_ + "EndTime", getEndTime());
        _s_.appendLong(_pn_ + "NextExpectedTime", getNextExpectedTime());
        _s_.appendLong(_pn_ + "ExpectedTime", getExpectedTime());
        _s_.appendLong(_pn_ + "HappenTime", getHappenTime());
        _s_.appendInt(_pn_ + "MissfirePolicy", getMissfirePolicy());
        _s_.appendString(_pn_ + "OneByOneKey", getOneByOneKey());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Delay", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Period", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "RemainTimes", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "HappenTimes", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "StartTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "EndTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "NextExpectedTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "ExpectedTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "HappenTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(10, "MissfirePolicy", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(11, "OneByOneKey", "string", "", ""));
        return _v_;
    }
}
