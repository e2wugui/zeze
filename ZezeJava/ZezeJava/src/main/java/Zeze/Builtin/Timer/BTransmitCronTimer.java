// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransmitCronTimer extends Zeze.Transaction.Bean implements BTransmitCronTimerReadOnly {
    public static final long TYPEID = 2513103118161345176L;

    private String _TimerId;
    private final Zeze.Transaction.Collections.CollOne<Zeze.Builtin.Timer.BCronTimer> _CronTimer;
    private String _HandleClass;
    private String _CustomClass;
    private Zeze.Net.Binary _CustomBean;
    private long _LoginVersion;
    private boolean _Hot;

    @Override
    public String getTimerId() {
        if (!isManaged())
            return _TimerId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _TimerId;
        var log = (Log__TimerId)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _TimerId;
    }

    public void setTimerId(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _TimerId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__TimerId(this, 1, _v_));
    }

    public Zeze.Builtin.Timer.BCronTimer getCronTimer() {
        return _CronTimer.getValue();
    }

    public void setCronTimer(Zeze.Builtin.Timer.BCronTimer _v_) {
        _CronTimer.setValue(_v_);
    }

    @Override
    public Zeze.Builtin.Timer.BCronTimerReadOnly getCronTimerReadOnly() {
        return _CronTimer.getValue();
    }

    @Override
    public String getHandleClass() {
        if (!isManaged())
            return _HandleClass;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _HandleClass;
        var log = (Log__HandleClass)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _HandleClass;
    }

    public void setHandleClass(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _HandleClass = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__HandleClass(this, 3, _v_));
    }

    @Override
    public String getCustomClass() {
        if (!isManaged())
            return _CustomClass;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _CustomClass;
        var log = (Log__CustomClass)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _CustomClass;
    }

    public void setCustomClass(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CustomClass = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__CustomClass(this, 4, _v_));
    }

    @Override
    public Zeze.Net.Binary getCustomBean() {
        if (!isManaged())
            return _CustomBean;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _CustomBean;
        var log = (Log__CustomBean)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _CustomBean;
    }

    public void setCustomBean(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _CustomBean = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__CustomBean(this, 5, _v_));
    }

    @Override
    public long getLoginVersion() {
        if (!isManaged())
            return _LoginVersion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _LoginVersion;
        var log = (Log__LoginVersion)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _LoginVersion;
    }

    public void setLoginVersion(long _v_) {
        if (!isManaged()) {
            _LoginVersion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__LoginVersion(this, 6, _v_));
    }

    @Override
    public boolean isHot() {
        if (!isManaged())
            return _Hot;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Hot;
        var log = (Log__Hot)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _Hot;
    }

    public void setHot(boolean _v_) {
        if (!isManaged()) {
            _Hot = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Hot(this, 7, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTransmitCronTimer() {
        _TimerId = "";
        _CronTimer = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Timer.BCronTimer(), Zeze.Builtin.Timer.BCronTimer.class);
        _CronTimer.variableId(2);
        _HandleClass = "";
        _CustomClass = "";
        _CustomBean = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BTransmitCronTimer(String _TimerId_, String _HandleClass_, String _CustomClass_, Zeze.Net.Binary _CustomBean_, long _LoginVersion_, boolean _Hot_) {
        if (_TimerId_ == null)
            _TimerId_ = "";
        _TimerId = _TimerId_;
        _CronTimer = new Zeze.Transaction.Collections.CollOne<>(new Zeze.Builtin.Timer.BCronTimer(), Zeze.Builtin.Timer.BCronTimer.class);
        _CronTimer.variableId(2);
        if (_HandleClass_ == null)
            _HandleClass_ = "";
        _HandleClass = _HandleClass_;
        if (_CustomClass_ == null)
            _CustomClass_ = "";
        _CustomClass = _CustomClass_;
        if (_CustomBean_ == null)
            _CustomBean_ = Zeze.Net.Binary.Empty;
        _CustomBean = _CustomBean_;
        _LoginVersion = _LoginVersion_;
        _Hot = _Hot_;
    }

    @Override
    public void reset() {
        setTimerId("");
        _CronTimer.reset();
        setHandleClass("");
        setCustomClass("");
        setCustomBean(Zeze.Net.Binary.Empty);
        setLoginVersion(0);
        setHot(false);
        _unknown_ = null;
    }

    public void assign(BTransmitCronTimer _o_) {
        setTimerId(_o_.getTimerId());
        _CronTimer.assign(_o_._CronTimer);
        setHandleClass(_o_.getHandleClass());
        setCustomClass(_o_.getCustomClass());
        setCustomBean(_o_.getCustomBean());
        setLoginVersion(_o_.getLoginVersion());
        setHot(_o_.isHot());
        _unknown_ = _o_._unknown_;
    }

    public BTransmitCronTimer copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransmitCronTimer copy() {
        var _c_ = new BTransmitCronTimer();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransmitCronTimer _a_, BTransmitCronTimer _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__TimerId extends Zeze.Transaction.Logs.LogString {
        public Log__TimerId(BTransmitCronTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._TimerId = value; }
    }

    private static final class Log__HandleClass extends Zeze.Transaction.Logs.LogString {
        public Log__HandleClass(BTransmitCronTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._HandleClass = value; }
    }

    private static final class Log__CustomClass extends Zeze.Transaction.Logs.LogString {
        public Log__CustomClass(BTransmitCronTimer _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._CustomClass = value; }
    }

    private static final class Log__CustomBean extends Zeze.Transaction.Logs.LogBinary {
        public Log__CustomBean(BTransmitCronTimer _b_, int _i_, Zeze.Net.Binary _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._CustomBean = value; }
    }

    private static final class Log__LoginVersion extends Zeze.Transaction.Logs.LogLong {
        public Log__LoginVersion(BTransmitCronTimer _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._LoginVersion = value; }
    }

    private static final class Log__Hot extends Zeze.Transaction.Logs.LogBool {
        public Log__Hot(BTransmitCronTimer _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BTransmitCronTimer)getBelong())._Hot = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Timer.BTransmitCronTimer: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("TimerId=").append(getTimerId()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("CronTimer=").append(System.lineSeparator());
        _CronTimer.buildString(_s_, _l_ + 4);
        _s_.append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("HandleClass=").append(getHandleClass()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("CustomClass=").append(getCustomClass()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("CustomBean=").append(getCustomBean()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("LoginVersion=").append(getLoginVersion()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Hot=").append(isHot()).append(System.lineSeparator());
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
            String _x_ = getTimerId();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _CronTimer.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = getHandleClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getCustomClass();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            var _x_ = getCustomBean();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            long _x_ = getLoginVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isHot();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setTimerId(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _o_.ReadBean(_CronTimer, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setHandleClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setCustomClass(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setCustomBean(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setLoginVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setHot(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTransmitCronTimer))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransmitCronTimer)_o_;
        if (!getTimerId().equals(_b_.getTimerId()))
            return false;
        if (!_CronTimer.equals(_b_._CronTimer))
            return false;
        if (!getHandleClass().equals(_b_.getHandleClass()))
            return false;
        if (!getCustomClass().equals(_b_.getCustomClass()))
            return false;
        if (!getCustomBean().equals(_b_.getCustomBean()))
            return false;
        if (getLoginVersion() != _b_.getLoginVersion())
            return false;
        if (isHot() != _b_.isHot())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _CronTimer.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _CronTimer.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (_CronTimer.negativeCheck())
            return true;
        if (getLoginVersion() < 0)
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
                case 1: _TimerId = _v_.stringValue(); break;
                case 2: _CronTimer.followerApply(_v_); break;
                case 3: _HandleClass = _v_.stringValue(); break;
                case 4: _CustomClass = _v_.stringValue(); break;
                case 5: _CustomBean = _v_.binaryValue(); break;
                case 6: _LoginVersion = _v_.longValue(); break;
                case 7: _Hot = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTimerId(_r_.getString(_pn_ + "TimerId"));
        if (getTimerId() == null)
            setTimerId("");
        _p_.add("CronTimer");
        _CronTimer.decodeResultSet(_p_, _r_);
        _p_.remove(_p_.size() - 1);
        setHandleClass(_r_.getString(_pn_ + "HandleClass"));
        if (getHandleClass() == null)
            setHandleClass("");
        setCustomClass(_r_.getString(_pn_ + "CustomClass"));
        if (getCustomClass() == null)
            setCustomClass("");
        setCustomBean(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "CustomBean")));
        setLoginVersion(_r_.getLong(_pn_ + "LoginVersion"));
        setHot(_r_.getBoolean(_pn_ + "Hot"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "TimerId", getTimerId());
        _p_.add("CronTimer");
        _CronTimer.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        _s_.appendString(_pn_ + "HandleClass", getHandleClass());
        _s_.appendString(_pn_ + "CustomClass", getCustomClass());
        _s_.appendBinary(_pn_ + "CustomBean", getCustomBean());
        _s_.appendLong(_pn_ + "LoginVersion", getLoginVersion());
        _s_.appendBoolean(_pn_ + "Hot", isHot());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TimerId", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "CronTimer", "Zeze.Builtin.Timer.BCronTimer", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "HandleClass", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "CustomClass", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "CustomBean", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "LoginVersion", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "Hot", "bool", "", ""));
        return _v_;
    }
}
