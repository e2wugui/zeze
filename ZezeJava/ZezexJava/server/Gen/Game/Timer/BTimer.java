// auto-generated @formatter:off
package Game.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"EqualsAndHashcode", "ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTimer extends Zeze.Transaction.Bean implements BTimerReadOnly {
    public static final long TYPEID = 3400360918189463633L;

    private long _TimerId;
    private String _Name;
    private long _Delay; // 首次延迟时间(毫秒)
    private long _Period; // 周期时间(毫秒), -1表示只有首次延迟
    private long _RemainTimes; // -1 表示不限次数。

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final java.lang.invoke.VarHandle vh_TimerId;
    private static final java.lang.invoke.VarHandle vh_Name;
    private static final java.lang.invoke.VarHandle vh_Delay;
    private static final java.lang.invoke.VarHandle vh_Period;
    private static final java.lang.invoke.VarHandle vh_RemainTimes;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_TimerId = _l_.findVarHandle(BTimer.class, "_TimerId", long.class);
            vh_Name = _l_.findVarHandle(BTimer.class, "_Name", String.class);
            vh_Delay = _l_.findVarHandle(BTimer.class, "_Delay", long.class);
            vh_Period = _l_.findVarHandle(BTimer.class, "_Period", long.class);
            vh_RemainTimes = _l_.findVarHandle(BTimer.class, "_RemainTimes", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getTimerId() {
        if (!isManaged())
            return _TimerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimerId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _TimerId;
    }

    public void setTimerId(long _v_) {
        if (!isManaged()) {
            _TimerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_TimerId, _v_));
    }

    @Override
    public String getName() {
        if (!isManaged())
            return _Name;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Name;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 2);
        return log != null ? log.stringValue() : _Name;
    }

    public void setName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Name = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 2, vh_Name, _v_));
    }

    @Override
    public long getDelay() {
        if (!isManaged())
            return _Delay;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Delay;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _Delay;
    }

    public void setDelay(long _v_) {
        if (!isManaged()) {
            _Delay = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 3, vh_Delay, _v_));
    }

    @Override
    public long getPeriod() {
        if (!isManaged())
            return _Period;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Period;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _Period;
    }

    public void setPeriod(long _v_) {
        if (!isManaged()) {
            _Period = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 4, vh_Period, _v_));
    }

    @Override
    public long getRemainTimes() {
        if (!isManaged())
            return _RemainTimes;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _RemainTimes;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _RemainTimes;
    }

    public void setRemainTimes(long _v_) {
        if (!isManaged()) {
            _RemainTimes = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 5, vh_RemainTimes, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTimer() {
        _Name = "";
    }

    @SuppressWarnings("deprecation")
    public BTimer(long _TimerId_, @Nullable String _Name_, long _Delay_, long _Period_, long _RemainTimes_) {
        _TimerId = _TimerId_;
        if (_Name_ == null)
            _Name_ = "";
        _Name = _Name_;
        _Delay = _Delay_;
        _Period = _Period_;
        _RemainTimes = _RemainTimes_;
    }

    @Override
    public void reset() {
        setTimerId(0);
        setName("");
        setDelay(0);
        setPeriod(0);
        setRemainTimes(0);
        _unknown_ = null;
    }

    public void assign(BTimer _o_) {
        setTimerId(_o_.getTimerId());
        setName(_o_.getName());
        setDelay(_o_.getDelay());
        setPeriod(_o_.getPeriod());
        setRemainTimes(_o_.getRemainTimes());
        _unknown_ = _o_._unknown_;
    }

    public BTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTimer copy() {
        var _c_ = new BTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTimer _a_, BTimer _b_) {
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
        _s_.append("Game.Timer.BTimer: {\n");
        _s_.append(_i1_).append("TimerId=").append(getTimerId()).append(",\n");
        _s_.append(_i1_).append("Name=").append(getName()).append(",\n");
        _s_.append(_i1_).append("Delay=").append(getDelay()).append(",\n");
        _s_.append(_i1_).append("Period=").append(getPeriod()).append(",\n");
        _s_.append(_i1_).append("RemainTimes=").append(getRemainTimes()).append('\n');
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

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
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
            long _x_ = getTimerId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getDelay();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getPeriod();
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
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setTimerId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setDelay(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setPeriod(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setRemainTimes(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTimer)_o_;
        if (getTimerId() != _b_.getTimerId())
            return false;
        if (!getName().equals(_b_.getName()))
            return false;
        if (getDelay() != _b_.getDelay())
            return false;
        if (getPeriod() != _b_.getPeriod())
            return false;
        if (getRemainTimes() != _b_.getRemainTimes())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getTimerId() < 0)
            return true;
        if (getDelay() < 0)
            return true;
        if (getPeriod() < 0)
            return true;
        if (getRemainTimes() < 0)
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
                case 1: _TimerId = _v_.longValue(); break;
                case 2: _Name = _v_.stringValue(); break;
                case 3: _Delay = _v_.longValue(); break;
                case 4: _Period = _v_.longValue(); break;
                case 5: _RemainTimes = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerId(_r_.getLong(_pn_ + "TimerId"));
        setName(_r_.getString(_pn_ + "Name"));
        if (getName() == null)
            setName("");
        setDelay(_r_.getLong(_pn_ + "Delay"));
        setPeriod(_r_.getLong(_pn_ + "Period"));
        setRemainTimes(_r_.getLong(_pn_ + "RemainTimes"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "TimerId", getTimerId());
        _s_.appendString(_pn_ + "Name", getName());
        _s_.appendLong(_pn_ + "Delay", getDelay());
        _s_.appendLong(_pn_ + "Period", getPeriod());
        _s_.appendLong(_pn_ + "RemainTimes", getRemainTimes());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Name", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Delay", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Period", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "RemainTimes", "long", "", ""));
        return _v_;
    }
}
