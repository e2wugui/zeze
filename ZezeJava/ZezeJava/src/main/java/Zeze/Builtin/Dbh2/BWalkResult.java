// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BWalkResult extends Zeze.Transaction.Bean implements BWalkResultReadOnly {
    public static final long TYPEID = -5849543620068480348L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.Dbh2.BWalkKeyValue> _KeyValues;
    private boolean _BucketEnd;
    private boolean _BucketRefuse;

    private static final java.lang.invoke.VarHandle vh_BucketEnd;
    private static final java.lang.invoke.VarHandle vh_BucketRefuse;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_BucketEnd = _l_.findVarHandle(BWalkResult.class, "_BucketEnd", boolean.class);
            vh_BucketRefuse = _l_.findVarHandle(BWalkResult.class, "_BucketRefuse", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.Dbh2.BWalkKeyValue> getKeyValues() {
        return _KeyValues;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Dbh2.BWalkKeyValue, Zeze.Builtin.Dbh2.BWalkKeyValueReadOnly> getKeyValuesReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_KeyValues);
    }

    @Override
    public boolean isBucketEnd() {
        if (!isManaged())
            return _BucketEnd;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _BucketEnd;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _BucketEnd;
    }

    public void setBucketEnd(boolean _v_) {
        if (!isManaged()) {
            _BucketEnd = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_BucketEnd, _v_));
    }

    @Override
    public boolean isBucketRefuse() {
        if (!isManaged())
            return _BucketRefuse;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _BucketRefuse;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _BucketRefuse;
    }

    public void setBucketRefuse(boolean _v_) {
        if (!isManaged()) {
            _BucketRefuse = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 3, vh_BucketRefuse, _v_));
    }

    @SuppressWarnings("deprecation")
    public BWalkResult() {
        _KeyValues = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Dbh2.BWalkKeyValue.class);
        _KeyValues.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BWalkResult(boolean _BucketEnd_, boolean _BucketRefuse_) {
        _KeyValues = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.Dbh2.BWalkKeyValue.class);
        _KeyValues.variableId(1);
        _BucketEnd = _BucketEnd_;
        _BucketRefuse = _BucketRefuse_;
    }

    @Override
    public void reset() {
        _KeyValues.clear();
        setBucketEnd(false);
        setBucketRefuse(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkResult.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BWalkResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BWalkResult.Data)_o_);
    }

    public void assign(BWalkResult.Data _o_) {
        _KeyValues.clear();
        for (var _e_ : _o_._KeyValues) {
            var _v_ = new Zeze.Builtin.Dbh2.BWalkKeyValue();
            _v_.assign(_e_);
            _KeyValues.add(_v_);
        }
        setBucketEnd(_o_._BucketEnd);
        setBucketRefuse(_o_._BucketRefuse);
        _unknown_ = null;
    }

    public void assign(BWalkResult _o_) {
        _KeyValues.clear();
        for (var _e_ : _o_._KeyValues)
            _KeyValues.add(_e_.copy());
        setBucketEnd(_o_.isBucketEnd());
        setBucketRefuse(_o_.isBucketRefuse());
        _unknown_ = _o_._unknown_;
    }

    public BWalkResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWalkResult copy() {
        var _c_ = new BWalkResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BWalkResult _a_, BWalkResult _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Dbh2.BWalkResult: {\n");
        _s_.append(_i1_).append("KeyValues=[");
        if (!_KeyValues.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _KeyValues) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_KeyValues.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("BucketEnd=").append(isBucketEnd()).append(",\n");
        _s_.append(_i1_).append("BucketRefuse=").append(isBucketRefuse()).append('\n');
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
            var _x_ = _KeyValues;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = isBucketEnd();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            boolean _x_ = isBucketRefuse();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
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
            var _x_ = _KeyValues;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Dbh2.BWalkKeyValue(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setBucketEnd(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setBucketRefuse(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BWalkResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BWalkResult)_o_;
        if (!_KeyValues.equals(_b_._KeyValues))
            return false;
        if (isBucketEnd() != _b_.isBucketEnd())
            return false;
        if (isBucketRefuse() != _b_.isBucketRefuse())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _KeyValues.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _KeyValues.initRootInfoWithRedo(_r_, this);
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
                case 1: _KeyValues.followerApply(_v_); break;
                case 2: _BucketEnd = _v_.booleanValue(); break;
                case 3: _BucketRefuse = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_KeyValues, Zeze.Builtin.Dbh2.BWalkKeyValue.class, _r_.getString(_pn_ + "KeyValues"));
        setBucketEnd(_r_.getBoolean(_pn_ + "BucketEnd"));
        setBucketRefuse(_r_.getBoolean(_pn_ + "BucketRefuse"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "KeyValues", Zeze.Serialize.Helper.encodeJson(_KeyValues));
        _s_.appendBoolean(_pn_ + "BucketEnd", isBucketEnd());
        _s_.appendBoolean(_pn_ + "BucketRefuse", isBucketRefuse());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "KeyValues", "list", "", "Zeze.Builtin.Dbh2.BWalkKeyValue"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "BucketEnd", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "BucketRefuse", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5849543620068480348L;

    private java.util.ArrayList<Zeze.Builtin.Dbh2.BWalkKeyValue.Data> _KeyValues;
    private boolean _BucketEnd;
    private boolean _BucketRefuse;

    public java.util.ArrayList<Zeze.Builtin.Dbh2.BWalkKeyValue.Data> getKeyValues() {
        return _KeyValues;
    }

    public void setKeyValues(java.util.ArrayList<Zeze.Builtin.Dbh2.BWalkKeyValue.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _KeyValues = _v_;
    }

    public boolean isBucketEnd() {
        return _BucketEnd;
    }

    public void setBucketEnd(boolean _v_) {
        _BucketEnd = _v_;
    }

    public boolean isBucketRefuse() {
        return _BucketRefuse;
    }

    public void setBucketRefuse(boolean _v_) {
        _BucketRefuse = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _KeyValues = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.Dbh2.BWalkKeyValue.Data> _KeyValues_, boolean _BucketEnd_, boolean _BucketRefuse_) {
        if (_KeyValues_ == null)
            _KeyValues_ = new java.util.ArrayList<>();
        _KeyValues = _KeyValues_;
        _BucketEnd = _BucketEnd_;
        _BucketRefuse = _BucketRefuse_;
    }

    @Override
    public void reset() {
        _KeyValues.clear();
        _BucketEnd = false;
        _BucketRefuse = false;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkResult toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BWalkResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BWalkResult)_o_);
    }

    public void assign(BWalkResult _o_) {
        _KeyValues.clear();
        for (var _e_ : _o_._KeyValues) {
            var _v_ = new Zeze.Builtin.Dbh2.BWalkKeyValue.Data();
            _v_.assign(_e_);
            _KeyValues.add(_v_);
        }
        _BucketEnd = _o_.isBucketEnd();
        _BucketRefuse = _o_.isBucketRefuse();
    }

    public void assign(BWalkResult.Data _o_) {
        _KeyValues.clear();
        for (var _e_ : _o_._KeyValues)
            _KeyValues.add(_e_.copy());
        _BucketEnd = _o_._BucketEnd;
        _BucketRefuse = _o_._BucketRefuse;
    }

    @Override
    public BWalkResult.Data copy() {
        var _c_ = new BWalkResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BWalkResult.Data _a_, BWalkResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWalkResult.Data clone() {
        return (BWalkResult.Data)super.clone();
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Dbh2.BWalkResult: {\n");
        _s_.append(_i1_).append("KeyValues=[");
        if (!_KeyValues.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _KeyValues) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_KeyValues.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("BucketEnd=").append(_BucketEnd).append(",\n");
        _s_.append(_i1_).append("BucketRefuse=").append(_BucketRefuse).append('\n');
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
            var _x_ = _KeyValues;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            boolean _x_ = _BucketEnd;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            boolean _x_ = _BucketRefuse;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _KeyValues;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.Dbh2.BWalkKeyValue.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _BucketEnd = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _BucketRefuse = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BWalkResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BWalkResult.Data)_o_;
        if (!_KeyValues.equals(_b_._KeyValues))
            return false;
        if (_BucketEnd != _b_._BucketEnd)
            return false;
        if (_BucketRefuse != _b_._BucketRefuse)
            return false;
        return true;
    }
}
}
