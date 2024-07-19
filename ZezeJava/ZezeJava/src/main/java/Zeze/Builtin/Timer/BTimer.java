// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTimer extends Zeze.Transaction.Bean implements BTimerReadOnly {
    public static final long TYPEID = -3755541261968580150L;

    private String _TimerName; // 用户指定的timerId(用户指定的,或"@"+Base64编码的自动分配ID)
    private String _HandleName; // 用户实现Zeze.Component.TimerHandle接口的完整类名
    private final Zeze.Transaction.DynamicBean _TimerObj;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BCronTimer = -6995089347718168392L;
    public static final long DynamicTypeId_TimerObj_Zeze_Builtin_Timer_BSimpleTimer = 1832177636612857692L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_TimerObj() {
        return new Zeze.Transaction.DynamicBean(3, BTimer::getSpecialTypeIdFromBean_3, BTimer::createBeanFromSpecialTypeId_3);
    }

    public static long getSpecialTypeIdFromBean_3(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -6995089347718168392L)
            return -6995089347718168392L; // Zeze.Builtin.Timer.BCronTimer
        if (_t_ == 1832177636612857692L)
            return 1832177636612857692L; // Zeze.Builtin.Timer.BSimpleTimer
        throw new UnsupportedOperationException("Unknown Bean! dynamic@Zeze.Builtin.Timer.BTimer:TimerObj");
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_3(long _t_) {
        if (_t_ == -6995089347718168392L)
            return new Zeze.Builtin.Timer.BCronTimer();
        if (_t_ == 1832177636612857692L)
            return new Zeze.Builtin.Timer.BSimpleTimer();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _CustomData;

    public static Zeze.Transaction.DynamicBean newDynamicBean_CustomData() {
        return new Zeze.Transaction.DynamicBean(4, Zeze.Component.Timer::getSpecialTypeIdFromBean, Zeze.Component.Timer::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_4(Zeze.Transaction.Bean _b_) {
        return Zeze.Component.Timer.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_4(long _t_) {
        return Zeze.Component.Timer.createBeanFromSpecialTypeId(_t_);
    }

    private long _ConcurrentFireSerialNo; // 触发定时器后自增的序列号, 用来避免并发timer触发

    private transient Object __zeze_map_key__;

    @Override
    public Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    @Override
    public String getTimerName() {
        if (!isManaged())
            return _TimerName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimerName;
        var log = (Log__TimerName)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _TimerName;
    }

    public void setTimerName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TimerName(this, 1, _v_));
    }

    @Override
    public String getHandleName() {
        if (!isManaged())
            return _HandleName;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HandleName;
        var log = (Log__HandleName)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _HandleName;
    }

    public void setHandleName(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleName = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HandleName(this, 2, _v_));
    }

    public Zeze.Transaction.DynamicBean getTimerObj() {
        return _TimerObj;
    }

    public Zeze.Builtin.Timer.BCronTimer getTimerObj_Zeze_Builtin_Timer_BCronTimer() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BCronTimer _v_) {
        _TimerObj.setBean(_v_);
    }

    public Zeze.Builtin.Timer.BSimpleTimer getTimerObj_Zeze_Builtin_Timer_BSimpleTimer() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
    }

    public void setTimerObj(Zeze.Builtin.Timer.BSimpleTimer _v_) {
        _TimerObj.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getTimerObjReadOnly() {
        return _TimerObj;
    }

    @Override
    public Zeze.Builtin.Timer.BCronTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BCronTimerReadOnly() {
        return (Zeze.Builtin.Timer.BCronTimer)_TimerObj.getBean();
    }

    @Override
    public Zeze.Builtin.Timer.BSimpleTimerReadOnly getTimerObj_Zeze_Builtin_Timer_BSimpleTimerReadOnly() {
        return (Zeze.Builtin.Timer.BSimpleTimer)_TimerObj.getBean();
    }

    public Zeze.Transaction.DynamicBean getCustomData() {
        return _CustomData;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly() {
        return _CustomData;
    }

    @Override
    public long getConcurrentFireSerialNo() {
        if (!isManaged())
            return _ConcurrentFireSerialNo;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ConcurrentFireSerialNo;
        var log = (Log__ConcurrentFireSerialNo)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _ConcurrentFireSerialNo;
    }

    public void setConcurrentFireSerialNo(long _v_) {
        if (!isManaged()) {
            _ConcurrentFireSerialNo = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ConcurrentFireSerialNo(this, 5, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTimer() {
        _TimerName = "";
        _HandleName = "";
        _TimerObj = newDynamicBean_TimerObj();
        _CustomData = newDynamicBean_CustomData();
    }

    @SuppressWarnings("deprecation")
    public BTimer(String _TimerName_, String _HandleName_, long _ConcurrentFireSerialNo_) {
        if (_TimerName_ == null)
            _TimerName_ = "";
        _TimerName = _TimerName_;
        if (_HandleName_ == null)
            _HandleName_ = "";
        _HandleName = _HandleName_;
        _TimerObj = newDynamicBean_TimerObj();
        _CustomData = newDynamicBean_CustomData();
        _ConcurrentFireSerialNo = _ConcurrentFireSerialNo_;
    }

    @Override
    public void reset() {
        setTimerName("");
        setHandleName("");
        _TimerObj.reset();
        _CustomData.reset();
        setConcurrentFireSerialNo(0);
        _unknown_ = null;
    }

    public void assign(BTimer _o_) {
        setTimerName(_o_.getTimerName());
        setHandleName(_o_.getHandleName());
        _TimerObj.assign(_o_._TimerObj);
        _CustomData.assign(_o_._CustomData);
        setConcurrentFireSerialNo(_o_.getConcurrentFireSerialNo());
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

    private static final class Log__TimerName extends Zeze.Transaction.Logs.LogString {
        public Log__TimerName(BTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTimer)getBelong())._TimerName = value; }
    }

    private static final class Log__HandleName extends Zeze.Transaction.Logs.LogString {
        public Log__HandleName(BTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTimer)getBelong())._HandleName = value; }
    }

    private static final class Log__ConcurrentFireSerialNo extends Zeze.Transaction.Logs.LogLong {
        public Log__ConcurrentFireSerialNo(BTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTimer)getBelong())._ConcurrentFireSerialNo = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Timer.BTimer: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TimerName=").append(getTimerName()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("HandleName=").append(getHandleName()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TimerObj=").append(System.lineSeparator());
        _TimerObj.getBean().buildString(_s_, _l_ + 4);
        _s_.append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("CustomData=").append(System.lineSeparator());
        _CustomData.getBean().buildString(_s_, _l_ + 4);
        _s_.append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ConcurrentFireSerialNo=").append(getConcurrentFireSerialNo()).append(System.lineSeparator());
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
            String _x_ = getTimerName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getHandleName();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = _TimerObj;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _CustomData;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            long _x_ = getConcurrentFireSerialNo();
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
            setTimerName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setHandleName(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _o_.ReadDynamic(_TimerObj, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _o_.ReadDynamic(_CustomData, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setConcurrentFireSerialNo(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTimer)_o_;
        if (!getTimerName().equals(_b_.getTimerName()))
            return false;
        if (!getHandleName().equals(_b_.getHandleName()))
            return false;
        if (!_TimerObj.equals(_b_._TimerObj))
            return false;
        if (!_CustomData.equals(_b_._CustomData))
            return false;
        if (getConcurrentFireSerialNo() != _b_.getConcurrentFireSerialNo())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _TimerObj.initRootInfo(_r_, this);
        _CustomData.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _TimerObj.initRootInfoWithRedo(_r_, this);
        _CustomData.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_TimerObj.negativeCheck())
            return true;
        if (getConcurrentFireSerialNo() < 0)
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
                case 1: _TimerName = _v_.stringValue(); break;
                case 2: _HandleName = _v_.stringValue(); break;
                case 3: _TimerObj.followerApply(_v_); break;
                case 4: _CustomData.followerApply(_v_); break;
                case 5: _ConcurrentFireSerialNo = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerName(_r_.getString(_pn_ + "TimerName"));
        if (getTimerName() == null)
            setTimerName("");
        setHandleName(_r_.getString(_pn_ + "HandleName"));
        if (getHandleName() == null)
            setHandleName("");
        Zeze.Serialize.Helper.decodeJsonDynamic(_TimerObj, _r_.getString(_pn_ + "TimerObj"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_CustomData, _r_.getString(_pn_ + "CustomData"));
        setConcurrentFireSerialNo(_r_.getLong(_pn_ + "ConcurrentFireSerialNo"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TimerName", getTimerName());
        _s_.appendString(_pn_ + "HandleName", getHandleName());
        _s_.appendString(_pn_ + "TimerObj", Zeze.Serialize.Helper.encodeJson(_TimerObj));
        _s_.appendString(_pn_ + "CustomData", Zeze.Serialize.Helper.encodeJson(_CustomData));
        _s_.appendLong(_pn_ + "ConcurrentFireSerialNo", getConcurrentFireSerialNo());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "HandleName", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimerObj", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "CustomData", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "ConcurrentFireSerialNo", "long", "", ""));
        return _v_;
    }
}
