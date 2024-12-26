// auto-generated @formatter:off
package Zeze.Builtin.LogService;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BResult extends Zeze.Transaction.Bean implements BResultReadOnly {
    public static final long TYPEID = 5146109133177652644L;

    private final Zeze.Transaction.Collections.PList2<Zeze.Builtin.LogService.BLog> _Logs;
    private boolean _Remain;

    private static final java.lang.invoke.VarHandle vh_Remain;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Remain = _l_.findVarHandle(BResult.class, "_Remain", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Builtin.LogService.BLog> getLogs() {
        return _Logs;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.LogService.BLog, Zeze.Builtin.LogService.BLogReadOnly> getLogsReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_Logs);
    }

    @Override
    public boolean isRemain() {
        if (!isManaged())
            return _Remain;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Remain;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Remain;
    }

    public void setRemain(boolean _v_) {
        if (!isManaged()) {
            _Remain = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_Remain, _v_));
    }

    @SuppressWarnings("deprecation")
    public BResult() {
        _Logs = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.LogService.BLog.class);
        _Logs.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BResult(boolean _Remain_) {
        _Logs = new Zeze.Transaction.Collections.PList2<>(Zeze.Builtin.LogService.BLog.class);
        _Logs.variableId(1);
        _Remain = _Remain_;
    }

    @Override
    public void reset() {
        _Logs.clear();
        setRemain(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.LogService.BResult.Data toData() {
        var _d_ = new Zeze.Builtin.LogService.BResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.LogService.BResult.Data)_o_);
    }

    public void assign(BResult.Data _o_) {
        _Logs.clear();
        for (var _e_ : _o_._Logs) {
            var _v_ = new Zeze.Builtin.LogService.BLog();
            _v_.assign(_e_);
            _Logs.add(_v_);
        }
        setRemain(_o_._Remain);
        _unknown_ = null;
    }

    public void assign(BResult _o_) {
        _Logs.clear();
        for (var _e_ : _o_._Logs)
            _Logs.add(_e_.copy());
        setRemain(_o_.isRemain());
        _unknown_ = _o_._unknown_;
    }

    public BResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BResult copy() {
        var _c_ = new BResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BResult _a_, BResult _b_) {
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
        _s_.append("Zeze.Builtin.LogService.BResult: {\n");
        _s_.append(_i1_).append("Logs=[");
        if (!_Logs.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Logs) {
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Remain=").append(isRemain()).append('\n');
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
            var _x_ = _Logs;
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
            boolean _x_ = isRemain();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            var _x_ = _Logs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.LogService.BLog(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setRemain(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BResult)_o_;
        if (!_Logs.equals(_b_._Logs))
            return false;
        if (isRemain() != _b_.isRemain())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Logs.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Logs.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _Logs) {
            if (_v_.negativeCheck())
                return true;
        }
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
                case 1: _Logs.followerApply(_v_); break;
                case 2: _Remain = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Logs, Zeze.Builtin.LogService.BLog.class, _r_.getString(_pn_ + "Logs"));
        setRemain(_r_.getBoolean(_pn_ + "Remain"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Logs", Zeze.Serialize.Helper.encodeJson(_Logs));
        _s_.appendBoolean(_pn_ + "Remain", isRemain());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Logs", "list", "", "Zeze.Builtin.LogService.BLog"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Remain", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = 5146109133177652644L;

    private java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> _Logs;
    private boolean _Remain;

    public java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> getLogs() {
        return _Logs;
    }

    public void setLogs(java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _Logs = _v_;
    }

    public boolean isRemain() {
        return _Remain;
    }

    public void setRemain(boolean _v_) {
        _Remain = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _Logs = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Zeze.Builtin.LogService.BLog.Data> _Logs_, boolean _Remain_) {
        if (_Logs_ == null)
            _Logs_ = new java.util.ArrayList<>();
        _Logs = _Logs_;
        _Remain = _Remain_;
    }

    @Override
    public void reset() {
        _Logs.clear();
        _Remain = false;
    }

    @Override
    public Zeze.Builtin.LogService.BResult toBean() {
        var _b_ = new Zeze.Builtin.LogService.BResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BResult)_o_);
    }

    public void assign(BResult _o_) {
        _Logs.clear();
        for (var _e_ : _o_._Logs) {
            var _v_ = new Zeze.Builtin.LogService.BLog.Data();
            _v_.assign(_e_);
            _Logs.add(_v_);
        }
        _Remain = _o_.isRemain();
    }

    public void assign(BResult.Data _o_) {
        _Logs.clear();
        for (var _e_ : _o_._Logs)
            _Logs.add(_e_.copy());
        _Remain = _o_._Remain;
    }

    @Override
    public BResult.Data copy() {
        var _c_ = new BResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BResult.Data _a_, BResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BResult.Data clone() {
        return (BResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.LogService.BResult: {\n");
        _s_.append(_i1_).append("Logs=[");
        if (!_Logs.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Logs) {
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("Remain=").append(_Remain).append('\n');
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
            var _x_ = _Logs;
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
            boolean _x_ = _Remain;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
            var _x_ = _Logs;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new Zeze.Builtin.LogService.BLog.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Remain = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BResult.Data)_o_;
        if (!_Logs.equals(_b_._Logs))
            return false;
        if (_Remain != _b_._Remain)
            return false;
        return true;
    }
}
}
