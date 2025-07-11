// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BSplitPut extends Zeze.Transaction.Bean implements BSplitPutReadOnly {
    public static final long TYPEID = 5420980520394401381L;

    private boolean _fromTransaction;
    private final Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> _Puts; // 包含delete，用Binary.Empty表示。

    private static final java.lang.invoke.VarHandle vh_fromTransaction;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_fromTransaction = _l_.findVarHandle(BSplitPut.class, "_fromTransaction", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public boolean isFromTransaction() {
        if (!isManaged())
            return _fromTransaction;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _fromTransaction;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _fromTransaction;
    }

    public void setFromTransaction(boolean _v_) {
        if (!isManaged()) {
            _fromTransaction = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 1, vh_fromTransaction, _v_));
    }

    public Zeze.Transaction.Collections.PMap1<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Zeze.Net.Binary, Zeze.Net.Binary> getPutsReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_Puts);
    }

    @SuppressWarnings("deprecation")
    public BSplitPut() {
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BSplitPut(boolean _fromTransaction_) {
        _fromTransaction = _fromTransaction_;
        _Puts = new Zeze.Transaction.Collections.PMap1<>(Zeze.Net.Binary.class, Zeze.Net.Binary.class);
        _Puts.variableId(2);
    }

    @Override
    public void reset() {
        setFromTransaction(false);
        _Puts.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BSplitPut.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BSplitPut.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BSplitPut.Data)_o_);
    }

    public void assign(BSplitPut.Data _o_) {
        setFromTransaction(_o_._fromTransaction);
        _Puts.clear();
        _Puts.putAll(_o_._Puts);
        _unknown_ = null;
    }

    public void assign(BSplitPut _o_) {
        setFromTransaction(_o_.isFromTransaction());
        _Puts.assign(_o_._Puts);
        _unknown_ = _o_._unknown_;
    }

    public BSplitPut copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSplitPut copy() {
        var _c_ = new BSplitPut();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSplitPut _a_, BSplitPut _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.BSplitPut: {\n");
        _s_.append(_i1_).append("fromTransaction=").append(isFromTransaction()).append(",\n");
        _s_.append(_i1_).append("Puts={");
        if (!_Puts.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Puts.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Puts.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            boolean _x_ = isFromTransaction();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            setFromTransaction(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSplitPut))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSplitPut)_o_;
        if (isFromTransaction() != _b_.isFromTransaction())
            return false;
        if (!_Puts.equals(_b_._Puts))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Puts.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Puts.initRootInfoWithRedo(_r_, this);
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
                case 1: _fromTransaction = _v_.booleanValue(); break;
                case 2: _Puts.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setFromTransaction(_r_.getBoolean(_pn_ + "fromTransaction"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "Puts", _Puts, _r_.getString(_pn_ + "Puts"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendBoolean(_pn_ + "fromTransaction", isFromTransaction());
        _s_.appendString(_pn_ + "Puts", Zeze.Serialize.Helper.encodeJson(_Puts));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "fromTransaction", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Puts", "map", "binary", "binary"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5420980520394401381L;

    private boolean _fromTransaction;
    private java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts; // 包含delete，用Binary.Empty表示。

    public boolean isFromTransaction() {
        return _fromTransaction;
    }

    public void setFromTransaction(boolean _v_) {
        _fromTransaction = _v_;
    }

    public java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> getPuts() {
        return _Puts;
    }

    public void setPuts(java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Puts = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Puts = new java.util.HashMap<>();
    }

    @SuppressWarnings("deprecation")
    public Data(boolean _fromTransaction_, java.util.HashMap<Zeze.Net.Binary, Zeze.Net.Binary> _Puts_) {
        _fromTransaction = _fromTransaction_;
        if (_Puts_ == null)
            _Puts_ = new java.util.HashMap<>();
        _Puts = _Puts_;
    }

    @Override
    public void reset() {
        _fromTransaction = false;
        _Puts.clear();
    }

    @Override
    public Zeze.Builtin.Dbh2.BSplitPut toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BSplitPut();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSplitPut)_o_);
    }

    public void assign(BSplitPut _o_) {
        _fromTransaction = _o_.isFromTransaction();
        _Puts.clear();
        _Puts.putAll(_o_._Puts);
    }

    public void assign(BSplitPut.Data _o_) {
        _fromTransaction = _o_._fromTransaction;
        _Puts.clear();
        _Puts.putAll(_o_._Puts);
    }

    @Override
    public BSplitPut.Data copy() {
        var _c_ = new BSplitPut.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSplitPut.Data _a_, BSplitPut.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSplitPut.Data clone() {
        return (BSplitPut.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BSplitPut: {\n");
        _s_.append(_i1_).append("fromTransaction=").append(_fromTransaction).append(",\n");
        _s_.append(_i1_).append("Puts={");
        if (!_Puts.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _Puts.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_Puts.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("}\n");
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
            boolean _x_ = _fromTransaction;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _Puts;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteBinary(_e_.getKey());
                    _o_.WriteBinary(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _fromTransaction = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Puts;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBinary(_s_);
                    var _v_ = _o_.ReadBinary(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
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
        if (!(_o_ instanceof BSplitPut.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSplitPut.Data)_o_;
        if (_fromTransaction != _b_._fromTransaction)
            return false;
        if (!_Puts.equals(_b_._Puts))
            return false;
        return true;
    }
}
}
