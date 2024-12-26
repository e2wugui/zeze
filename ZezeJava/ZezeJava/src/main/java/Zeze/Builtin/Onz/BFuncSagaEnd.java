// auto-generated @formatter:off
package Zeze.Builtin.Onz;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BFuncSagaEnd extends Zeze.Transaction.Bean implements BFuncSagaEndReadOnly {
    public static final long TYPEID = -5966280333833227562L;

    private long _OnzTid;
    private boolean _Cancel;
    private Zeze.Net.Binary _FuncArgument;

    private static final java.lang.invoke.VarHandle vh_OnzTid;
    private static final java.lang.invoke.VarHandle vh_Cancel;
    private static final java.lang.invoke.VarHandle vh_FuncArgument;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_OnzTid = _l_.findVarHandle(BFuncSagaEnd.class, "_OnzTid", long.class);
            vh_Cancel = _l_.findVarHandle(BFuncSagaEnd.class, "_Cancel", boolean.class);
            vh_FuncArgument = _l_.findVarHandle(BFuncSagaEnd.class, "_FuncArgument", Zeze.Net.Binary.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getOnzTid() {
        if (!isManaged())
            return _OnzTid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _OnzTid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _OnzTid;
    }

    public void setOnzTid(long _v_) {
        if (!isManaged()) {
            _OnzTid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_OnzTid, _v_));
    }

    @Override
    public boolean isCancel() {
        if (!isManaged())
            return _Cancel;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Cancel;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Cancel;
    }

    public void setCancel(boolean _v_) {
        if (!isManaged()) {
            _Cancel = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_Cancel, _v_));
    }

    @Override
    public Zeze.Net.Binary getFuncArgument() {
        if (!isManaged())
            return _FuncArgument;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _FuncArgument;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 3);
        return log != null ? log.value : _FuncArgument;
    }

    public void setFuncArgument(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _FuncArgument = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 3, vh_FuncArgument, _v_));
    }

    @SuppressWarnings("deprecation")
    public BFuncSagaEnd() {
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public BFuncSagaEnd(long _OnzTid_, boolean _Cancel_, Zeze.Net.Binary _FuncArgument_) {
        _OnzTid = _OnzTid_;
        _Cancel = _Cancel_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
    }

    @Override
    public void reset() {
        setOnzTid(0);
        setCancel(false);
        setFuncArgument(Zeze.Net.Binary.Empty);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSagaEnd.Data toData() {
        var _d_ = new Zeze.Builtin.Onz.BFuncSagaEnd.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Onz.BFuncSagaEnd.Data)_o_);
    }

    public void assign(BFuncSagaEnd.Data _o_) {
        setOnzTid(_o_._OnzTid);
        setCancel(_o_._Cancel);
        setFuncArgument(_o_._FuncArgument);
        _unknown_ = null;
    }

    public void assign(BFuncSagaEnd _o_) {
        setOnzTid(_o_.getOnzTid());
        setCancel(_o_.isCancel());
        setFuncArgument(_o_.getFuncArgument());
        _unknown_ = _o_._unknown_;
    }

    public BFuncSagaEnd copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BFuncSagaEnd copy() {
        var _c_ = new BFuncSagaEnd();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFuncSagaEnd _a_, BFuncSagaEnd _b_) {
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
        _s_.append("Zeze.Builtin.Onz.BFuncSagaEnd: {\n");
        _s_.append(_i1_).append("OnzTid=").append(getOnzTid()).append(",\n");
        _s_.append(_i1_).append("Cancel=").append(isCancel()).append(",\n");
        _s_.append(_i1_).append("FuncArgument=").append(getFuncArgument()).append('\n');
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
            long _x_ = getOnzTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isCancel();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = getFuncArgument();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
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
            setOnzTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setCancel(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setFuncArgument(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BFuncSagaEnd))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFuncSagaEnd)_o_;
        if (getOnzTid() != _b_.getOnzTid())
            return false;
        if (isCancel() != _b_.isCancel())
            return false;
        if (!getFuncArgument().equals(_b_.getFuncArgument()))
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getOnzTid() < 0)
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
                case 1: _OnzTid = _v_.longValue(); break;
                case 2: _Cancel = _v_.booleanValue(); break;
                case 3: _FuncArgument = _v_.binaryValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setOnzTid(_r_.getLong(_pn_ + "OnzTid"));
        setCancel(_r_.getBoolean(_pn_ + "Cancel"));
        setFuncArgument(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "FuncArgument")));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "OnzTid", getOnzTid());
        _s_.appendBoolean(_pn_ + "Cancel", isCancel());
        _s_.appendBinary(_pn_ + "FuncArgument", getFuncArgument());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "OnzTid", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Cancel", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "FuncArgument", "binary", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -5966280333833227562L;

    private long _OnzTid;
    private boolean _Cancel;
    private Zeze.Net.Binary _FuncArgument;

    public long getOnzTid() {
        return _OnzTid;
    }

    public void setOnzTid(long _v_) {
        _OnzTid = _v_;
    }

    public boolean isCancel() {
        return _Cancel;
    }

    public void setCancel(boolean _v_) {
        _Cancel = _v_;
    }

    public Zeze.Net.Binary getFuncArgument() {
        return _FuncArgument;
    }

    public void setFuncArgument(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _FuncArgument = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @SuppressWarnings("deprecation")
    public Data(long _OnzTid_, boolean _Cancel_, Zeze.Net.Binary _FuncArgument_) {
        _OnzTid = _OnzTid_;
        _Cancel = _Cancel_;
        if (_FuncArgument_ == null)
            _FuncArgument_ = Zeze.Net.Binary.Empty;
        _FuncArgument = _FuncArgument_;
    }

    @Override
    public void reset() {
        _OnzTid = 0;
        _Cancel = false;
        _FuncArgument = Zeze.Net.Binary.Empty;
    }

    @Override
    public Zeze.Builtin.Onz.BFuncSagaEnd toBean() {
        var _b_ = new Zeze.Builtin.Onz.BFuncSagaEnd();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BFuncSagaEnd)_o_);
    }

    public void assign(BFuncSagaEnd _o_) {
        _OnzTid = _o_.getOnzTid();
        _Cancel = _o_.isCancel();
        _FuncArgument = _o_.getFuncArgument();
    }

    public void assign(BFuncSagaEnd.Data _o_) {
        _OnzTid = _o_._OnzTid;
        _Cancel = _o_._Cancel;
        _FuncArgument = _o_._FuncArgument;
    }

    @Override
    public BFuncSagaEnd.Data copy() {
        var _c_ = new BFuncSagaEnd.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BFuncSagaEnd.Data _a_, BFuncSagaEnd.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BFuncSagaEnd.Data clone() {
        return (BFuncSagaEnd.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Onz.BFuncSagaEnd: {\n");
        _s_.append(_i1_).append("OnzTid=").append(_OnzTid).append(",\n");
        _s_.append(_i1_).append("Cancel=").append(_Cancel).append(",\n");
        _s_.append(_i1_).append("FuncArgument=").append(_FuncArgument).append('\n');
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
            long _x_ = _OnzTid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = _Cancel;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            var _x_ = _FuncArgument;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _OnzTid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Cancel = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _FuncArgument = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BFuncSagaEnd.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BFuncSagaEnd.Data)_o_;
        if (_OnzTid != _b_._OnzTid)
            return false;
        if (_Cancel != _b_._Cancel)
            return false;
        if (!_FuncArgument.equals(_b_._FuncArgument))
            return false;
        return true;
    }
}
}
