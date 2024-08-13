// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTransactionState extends Zeze.Transaction.Bean implements BTransactionStateReadOnly {
    public static final long TYPEID = 7092279656883376454L;

    private int _State;
    private final Zeze.Transaction.Collections.PList1<String> _Buckets;

    private static final java.lang.invoke.VarHandle vh_State;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_State = _l_.findVarHandle(BTransactionState.class, "_State", int.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getState() {
        if (!isManaged())
            return _State;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _State;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _State;
    }

    public void setState(int _v_) {
        if (!isManaged()) {
            _State = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_State, _v_));
    }

    public Zeze.Transaction.Collections.PList1<String> getBuckets() {
        return _Buckets;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<String> getBucketsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Buckets);
    }

    @SuppressWarnings("deprecation")
    public BTransactionState() {
        _Buckets = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Buckets.variableId(2);
    }

    @SuppressWarnings("deprecation")
    public BTransactionState(int _State_) {
        _State = _State_;
        _Buckets = new Zeze.Transaction.Collections.PList1<>(String.class);
        _Buckets.variableId(2);
    }

    @Override
    public void reset() {
        setState(0);
        _Buckets.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BTransactionState.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Commit.BTransactionState.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Commit.BTransactionState.Data)_o_);
    }

    public void assign(BTransactionState.Data _o_) {
        setState(_o_._State);
        _Buckets.clear();
        _Buckets.addAll(_o_._Buckets);
        _unknown_ = null;
    }

    public void assign(BTransactionState _o_) {
        setState(_o_.getState());
        _Buckets.assign(_o_._Buckets);
        _unknown_ = _o_._unknown_;
    }

    public BTransactionState copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTransactionState copy() {
        var _c_ = new BTransactionState();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransactionState _a_, BTransactionState _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.Commit.BTransactionState: {\n");
        _s_.append(_i1_).append("State=").append(getState()).append(",\n");
        _s_.append(_i1_).append("Buckets=[");
        if (!_Buckets.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Buckets) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            int _x_ = getState();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Buckets;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteString(_v_);
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
            setState(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Buckets;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTransactionState))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransactionState)_o_;
        if (getState() != _b_.getState())
            return false;
        if (!_Buckets.equals(_b_._Buckets))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Buckets.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Buckets.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getState() < 0)
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
                case 1: _State = _v_.intValue(); break;
                case 2: _Buckets.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setState(_r_.getInt(_pn_ + "State"));
        Zeze.Serialize.Helper.decodeJsonList(_Buckets, String.class, _r_.getString(_pn_ + "Buckets"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "State", getState());
        _s_.appendString(_pn_ + "Buckets", Zeze.Serialize.Helper.encodeJson(_Buckets));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "State", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Buckets", "list", "", "string"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 7092279656883376454L;

    private int _State;
    private java.util.ArrayList<String> _Buckets;

    public int getState() {
        return _State;
    }

    public void setState(int _v_) {
        _State = _v_;
    }

    public java.util.ArrayList<String> getBuckets() {
        return _Buckets;
    }

    public void setBuckets(java.util.ArrayList<String> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Buckets = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Buckets = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _State_, java.util.ArrayList<String> _Buckets_) {
        _State = _State_;
        if (_Buckets_ == null)
            _Buckets_ = new java.util.ArrayList<>();
        _Buckets = _Buckets_;
    }

    @Override
    public void reset() {
        _State = 0;
        _Buckets.clear();
    }

    @Override
    public Zeze.Builtin.Dbh2.Commit.BTransactionState toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Commit.BTransactionState();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTransactionState)_o_);
    }

    public void assign(BTransactionState _o_) {
        _State = _o_.getState();
        _Buckets.clear();
        _Buckets.addAll(_o_._Buckets);
    }

    public void assign(BTransactionState.Data _o_) {
        _State = _o_._State;
        _Buckets.clear();
        _Buckets.addAll(_o_._Buckets);
    }

    @Override
    public BTransactionState.Data copy() {
        var _c_ = new BTransactionState.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTransactionState.Data _a_, BTransactionState.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTransactionState.Data clone() {
        return (BTransactionState.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Commit.BTransactionState: {\n");
        _s_.append(_i1_).append("State=").append(_State).append(",\n");
        _s_.append(_i1_).append("Buckets=[");
        if (!_Buckets.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Buckets) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            int _x_ = _State;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _Buckets;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.LIST);
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
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _State = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            var _x_ = _Buckets;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadString(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
        if (!(_o_ instanceof BTransactionState.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTransactionState.Data)_o_;
        if (_State != _b_._State)
            return false;
        if (!_Buckets.equals(_b_._Buckets))
            return false;
        return true;
    }
}
}
