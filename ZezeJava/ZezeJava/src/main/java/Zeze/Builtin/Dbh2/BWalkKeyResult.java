// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BWalkKeyResult extends Zeze.Transaction.Bean implements BWalkKeyResultReadOnly {
    public static final long TYPEID = 7613011447108499443L;

    private final Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Keys;
    private boolean _BucketEnd;
    private boolean _BucketRefuse;

    private static final java.lang.invoke.VarHandle vh_BucketEnd;
    private static final java.lang.invoke.VarHandle vh_BucketRefuse;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_BucketEnd = _l_.findVarHandle(BWalkKeyResult.class, "_BucketEnd", boolean.class);
            vh_BucketRefuse = _l_.findVarHandle(BWalkKeyResult.class, "_BucketRefuse", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getKeys() {
        return _Keys;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getKeysReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Keys);
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
    public BWalkKeyResult() {
        _Keys = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Keys.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BWalkKeyResult(boolean _BucketEnd_, boolean _BucketRefuse_) {
        _Keys = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Keys.variableId(1);
        _BucketEnd = _BucketEnd_;
        _BucketRefuse = _BucketRefuse_;
    }

    @Override
    public void reset() {
        _Keys.clear();
        setBucketEnd(false);
        setBucketRefuse(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkKeyResult.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BWalkKeyResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BWalkKeyResult.Data)_o_);
    }

    public void assign(BWalkKeyResult.Data _o_) {
        _Keys.clear();
        _Keys.addAll(_o_._Keys);
        setBucketEnd(_o_._BucketEnd);
        setBucketRefuse(_o_._BucketRefuse);
        _unknown_ = null;
    }

    public void assign(BWalkKeyResult _o_) {
        _Keys.assign(_o_._Keys);
        setBucketEnd(_o_.isBucketEnd());
        setBucketRefuse(_o_.isBucketRefuse());
        _unknown_ = _o_._unknown_;
    }

    public BWalkKeyResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BWalkKeyResult copy() {
        var _c_ = new BWalkKeyResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BWalkKeyResult _a_, BWalkKeyResult _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.BWalkKeyResult: {\n");
        _s_.append(_i1_).append("Keys=[");
        if (!_Keys.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Keys) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
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
            var _x_ = _Keys;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteBinary(_v_);
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
            var _x_ = _Keys;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
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
        if (!(_o_ instanceof BWalkKeyResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BWalkKeyResult)_o_;
        if (!_Keys.equals(_b_._Keys))
            return false;
        if (isBucketEnd() != _b_.isBucketEnd())
            return false;
        if (isBucketRefuse() != _b_.isBucketRefuse())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Keys.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Keys.initRootInfoWithRedo(_r_, this);
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
                case 1: _Keys.followerApply(_v_); break;
                case 2: _BucketEnd = _v_.booleanValue(); break;
                case 3: _BucketRefuse = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Keys, Zeze.Net.Binary.class, _r_.getString(_pn_ + "Keys"));
        setBucketEnd(_r_.getBoolean(_pn_ + "BucketEnd"));
        setBucketRefuse(_r_.getBoolean(_pn_ + "BucketRefuse"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Keys", Zeze.Serialize.Helper.encodeJson(_Keys));
        _s_.appendBoolean(_pn_ + "BucketEnd", isBucketEnd());
        _s_.appendBoolean(_pn_ + "BucketRefuse", isBucketRefuse());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Keys", "list", "", "binary"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "BucketEnd", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "BucketRefuse", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7613011447108499443L;

    private java.util.ArrayList<Zeze.Net.Binary> _Keys;
    private boolean _BucketEnd;
    private boolean _BucketRefuse;

    public java.util.ArrayList<Zeze.Net.Binary> getKeys() {
        return _Keys;
    }

    public void setKeys(java.util.ArrayList<Zeze.Net.Binary> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Keys = _v_;
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
        _Keys = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Net.Binary> _Keys_, boolean _BucketEnd_, boolean _BucketRefuse_) {
        if (_Keys_ == null)
            _Keys_ = new java.util.ArrayList<>();
        _Keys = _Keys_;
        _BucketEnd = _BucketEnd_;
        _BucketRefuse = _BucketRefuse_;
    }

    @Override
    public void reset() {
        _Keys.clear();
        _BucketEnd = false;
        _BucketRefuse = false;
    }

    @Override
    public Zeze.Builtin.Dbh2.BWalkKeyResult toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BWalkKeyResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BWalkKeyResult)_o_);
    }

    public void assign(BWalkKeyResult _o_) {
        _Keys.clear();
        _Keys.addAll(_o_._Keys);
        _BucketEnd = _o_.isBucketEnd();
        _BucketRefuse = _o_.isBucketRefuse();
    }

    public void assign(BWalkKeyResult.Data _o_) {
        _Keys.clear();
        _Keys.addAll(_o_._Keys);
        _BucketEnd = _o_._BucketEnd;
        _BucketRefuse = _o_._BucketRefuse;
    }

    @Override
    public BWalkKeyResult.Data copy() {
        var _c_ = new BWalkKeyResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BWalkKeyResult.Data _a_, BWalkKeyResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BWalkKeyResult.Data clone() {
        return (BWalkKeyResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BWalkKeyResult: {\n");
        _s_.append(_i1_).append("Keys=[");
        if (!_Keys.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Keys) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
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
            var _x_ = _Keys;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteBinary(_v_);
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
            var _x_ = _Keys;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
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
        if (!(_o_ instanceof BWalkKeyResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BWalkKeyResult.Data)_o_;
        if (!_Keys.equals(_b_._Keys))
            return false;
        if (_BucketEnd != _b_._BucketEnd)
            return false;
        if (_BucketRefuse != _b_._BucketRefuse)
            return false;
        return true;
    }
}
}
