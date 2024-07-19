// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BCondition extends Zeze.Transaction.Bean implements BConditionReadOnly {
    public static final long TYPEID = -4711929821698256188L;

    public static final int ContainsAll = 0;
    public static final int ContainsAny = 1;
    public static final int ContainsNone = 2;

    private long _BeginTime;
    private long _EndTime;
    private final Zeze.Transaction.Collections.PList1<String> _Words;
    private int _ContainsType;
    private String _Pattern;

    @Override
    public long getBeginTime() {
        if (!isManaged())
            return _BeginTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _BeginTime;
        var log = (Log__BeginTime)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _BeginTime;
    }

    public void setBeginTime(long _v_) {
        if (!isManaged()) {
            _BeginTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__BeginTime(this, 1, _v_));
    }

    @Override
    public long getEndTime() {
        if (!isManaged())
            return _EndTime;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _EndTime;
        var log = (Log__EndTime)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _EndTime;
    }

    public void setEndTime(long _v_) {
        if (!isManaged()) {
            _EndTime = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__EndTime(this, 2, _v_));
    }

    public Zeze.Transaction.Collections.PList1<String> getWords() {
        return _Words;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getWordsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Words);
    }

    @Override
    public int getContainsType() {
        if (!isManaged())
            return _ContainsType;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ContainsType;
        var log = (Log__ContainsType)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _ContainsType;
    }

    public void setContainsType(int _v_) {
        if (!isManaged()) {
            _ContainsType = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ContainsType(this, 4, _v_));
    }

    @Override
    public String getPattern() {
        if (!isManaged())
            return _Pattern;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Pattern;
        var log = (Log__Pattern)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _Pattern;
    }

    public void setPattern(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _Pattern = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Pattern(this, 5, _v_));
    }

    @SuppressWarnings("deprecation")
    public BCondition() {
        _Words = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Words.variableId(3);
        _Pattern = "";
    }

    @SuppressWarnings("deprecation")
    public BCondition(long _BeginTime_, long _EndTime_, int _ContainsType_, String _Pattern_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        _Words = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Words.variableId(3);
        _ContainsType = _ContainsType_;
        if (_Pattern_ == null)
            _Pattern_ = "";
        _Pattern = _Pattern_;
    }

    @Override
    public void reset() {
        setBeginTime(0);
        setEndTime(0);
        _Words.clear();
        setContainsType(0);
        setPattern("");
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BCondition.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BCondition.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BCondition.Data)_o_);
    }

    public void assign(BCondition.Data _o_) {
        setBeginTime(_o_._BeginTime);
        setEndTime(_o_._EndTime);
        _Words.clear();
        _Words.addAll(_o_._Words);
        setContainsType(_o_._ContainsType);
        setPattern(_o_._Pattern);
        _unknown_ = null;
    }

    public void assign(BCondition _o_) {
        setBeginTime(_o_.getBeginTime());
        setEndTime(_o_.getEndTime());
        _Words.assign(_o_._Words);
        setContainsType(_o_.getContainsType());
        setPattern(_o_.getPattern());
        _unknown_ = _o_._unknown_;
    }

    public BCondition copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BCondition copy() {
        var _c_ = new BCondition();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCondition _a_, BCondition _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__BeginTime extends Zeze.Transaction.Logs.LogLong {
        public Log__BeginTime(BCondition _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCondition)getBelong())._BeginTime = value; }
    }

    private static final class Log__EndTime extends Zeze.Transaction.Logs.LogLong {
        public Log__EndTime(BCondition _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCondition)getBelong())._EndTime = value; }
    }

    private static final class Log__ContainsType extends Zeze.Transaction.Logs.LogInt {
        public Log__ContainsType(BCondition _b_, int _i_, int _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCondition)getBelong())._ContainsType = value; }
    }

    private static final class Log__Pattern extends Zeze.Transaction.Logs.LogString {
        public Log__Pattern(BCondition _b_, int _i_, String _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BCondition)getBelong())._Pattern = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.LogService.BCondition: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("BeginTime=").append(getBeginTime()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("EndTime=").append(getEndTime()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Words=[");
        if (!_Words.isEmpty()) {
            _s_.append(System.lineSeparator());
            _l_ += 4;
            for (var _v_ : _Words) {
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Item=").append(_v_).append(',').append(System.lineSeparator());
            }
            _l_ -= 4;
            _s_.append(Zeze.Util.Str.indent(_l_));
        }
        _s_.append(']').append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ContainsType=").append(getContainsType()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Pattern=").append(getPattern()).append(System.lineSeparator());
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
            long _x_ = getBeginTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getEndTime();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Words;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _x_ = getContainsType();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = getPattern();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
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
            setBeginTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setEndTime(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Words;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setContainsType(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setPattern(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BCondition))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BCondition)_o_;
        if (getBeginTime() != _b_.getBeginTime())
            return false;
        if (getEndTime() != _b_.getEndTime())
            return false;
        if (!_Words.equals(_b_._Words))
            return false;
        if (getContainsType() != _b_.getContainsType())
            return false;
        if (!getPattern().equals(_b_.getPattern()))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Words.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Words.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getBeginTime() < 0)
            return true;
        if (getEndTime() < 0)
            return true;
        if (getContainsType() < 0)
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
                case 1: _BeginTime = _v_.longValue(); break;
                case 2: _EndTime = _v_.longValue(); break;
                case 3: _Words.followerApply(_v_); break;
                case 4: _ContainsType = _v_.intValue(); break;
                case 5: _Pattern = _v_.stringValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setBeginTime(_r_.getLong(_pn_ + "BeginTime"));
        setEndTime(_r_.getLong(_pn_ + "EndTime"));
        Zeze.Serialize.Helper.decodeJsonList(_Words, String.class, _r_.getString(_pn_ + "Words"));
        setContainsType(_r_.getInt(_pn_ + "ContainsType"));
        setPattern(_r_.getString(_pn_ + "Pattern"));
        if (getPattern() == null)
            setPattern("");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "BeginTime", getBeginTime());
        _s_.appendLong(_pn_ + "EndTime", getEndTime());
        _s_.appendString(_pn_ + "Words", Zeze.Serialize.Helper.encodeJson(_Words));
        _s_.appendInt(_pn_ + "ContainsType", getContainsType());
        _s_.appendString(_pn_ + "Pattern", getPattern());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "BeginTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "EndTime", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "Words", "list", "", "string"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "ContainsType", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Pattern", "string", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4711929821698256188L;

    public static final int ContainsAll = 0;
    public static final int ContainsAny = 1;
    public static final int ContainsNone = 2;

    private long _BeginTime;
    private long _EndTime;
    private java.util.ArrayList<String> _Words;
    private int _ContainsType;
    private String _Pattern;

    public long getBeginTime() {
        return _BeginTime;
    }

    public void setBeginTime(long _v_) {
        _BeginTime = _v_;
    }

    public long getEndTime() {
        return _EndTime;
    }

    public void setEndTime(long _v_) {
        _EndTime = _v_;
    }

    public java.util.ArrayList<String> getWords() {
        return _Words;
    }

    public void setWords(java.util.ArrayList<String> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Words = _v_;
    }

    public int getContainsType() {
        return _ContainsType;
    }

    public void setContainsType(int _v_) {
        _ContainsType = _v_;
    }

    public String getPattern() {
        return _Pattern;
    }

    public void setPattern(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Pattern = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Words = new java.util.ArrayList<>();
        _Pattern = "";
    }

    @SuppressWarnings("deprecation")
    public Data(long _BeginTime_, long _EndTime_, java.util.ArrayList<String> _Words_, int _ContainsType_, String _Pattern_) {
        _BeginTime = _BeginTime_;
        _EndTime = _EndTime_;
        if (_Words_ == null)
            _Words_ = new java.util.ArrayList<>();
        _Words = _Words_;
        _ContainsType = _ContainsType_;
        if (_Pattern_ == null)
            _Pattern_ = "";
        _Pattern = _Pattern_;
    }

    @Override
    public void reset() {
        _BeginTime = 0;
        _EndTime = 0;
        _Words.clear();
        _ContainsType = 0;
        _Pattern = "";
    }

    @Override
    public Zeze.Builtin.LogService.BCondition toBean() {
        var _b_ = new Zeze.Builtin.LogService.BCondition();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BCondition)_o_);
    }

    public void assign(BCondition _o_) {
        _BeginTime = _o_.getBeginTime();
        _EndTime = _o_.getEndTime();
        _Words.clear();
        _Words.addAll(_o_._Words);
        _ContainsType = _o_.getContainsType();
        _Pattern = _o_.getPattern();
    }

    public void assign(BCondition.Data _o_) {
        _BeginTime = _o_._BeginTime;
        _EndTime = _o_._EndTime;
        _Words.clear();
        _Words.addAll(_o_._Words);
        _ContainsType = _o_._ContainsType;
        _Pattern = _o_._Pattern;
    }

    @Override
    public BCondition.Data copy() {
        var _c_ = new BCondition.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BCondition.Data _a_, BCondition.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BCondition.Data clone() {
        return (BCondition.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.LogService.BCondition: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("BeginTime=").append(_BeginTime).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("EndTime=").append(_EndTime).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Words=[");
        if (!_Words.isEmpty()) {
            _s_.append(System.lineSeparator());
            _l_ += 4;
            for (var _v_ : _Words) {
                _s_.append(Zeze.Util.Str.indent(_l_)).append("Item=").append(_v_).append(',').append(System.lineSeparator());
            }
            _l_ -= 4;
            _s_.append(Zeze.Util.Str.indent(_l_));
        }
        _s_.append(']').append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ContainsType=").append(_ContainsType).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Pattern=").append(_Pattern).append(System.lineSeparator());
        _l_ -= 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            long _x_ = _BeginTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = _EndTime;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _Words;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteString(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _x_ = _ContainsType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            String _x_ = _Pattern;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _BeginTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _EndTime = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            var _x_ = _Words;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _ContainsType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Pattern = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }
}
}
