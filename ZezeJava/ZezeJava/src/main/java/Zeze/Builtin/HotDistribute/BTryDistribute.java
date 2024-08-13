// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTryDistribute extends Zeze.Transaction.Bean implements BTryDistributeReadOnly {
    public static final long TYPEID = -555413257891539268L;

    private long _DistributeId;
    private boolean _AtomicAll;

    private static final java.lang.invoke.VarHandle vh_DistributeId;
    private static final java.lang.invoke.VarHandle vh_AtomicAll;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_DistributeId = _l_.findVarHandle(BTryDistribute.class, "_DistributeId", long.class);
            vh_AtomicAll = _l_.findVarHandle(BTryDistribute.class, "_AtomicAll", boolean.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getDistributeId() {
        if (!isManaged())
            return _DistributeId;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _DistributeId;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _DistributeId;
    }

    public void setDistributeId(long _v_) {
        if (!isManaged()) {
            _DistributeId = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_DistributeId, _v_));
    }

    @Override
    public boolean isAtomicAll() {
        if (!isManaged())
            return _AtomicAll;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _AtomicAll;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _AtomicAll;
    }

    public void setAtomicAll(boolean _v_) {
        if (!isManaged()) {
            _AtomicAll = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 2, vh_AtomicAll, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTryDistribute() {
    }

    @SuppressWarnings("deprecation")
    public BTryDistribute(long _DistributeId_, boolean _AtomicAll_) {
        _DistributeId = _DistributeId_;
        _AtomicAll = _AtomicAll_;
    }

    @Override
    public void reset() {
        setDistributeId(0);
        setAtomicAll(false);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BTryDistribute.Data toData() {
        var _d_ = new Zeze.Builtin.HotDistribute.BTryDistribute.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.HotDistribute.BTryDistribute.Data)_o_);
    }

    public void assign(BTryDistribute.Data _o_) {
        setDistributeId(_o_._DistributeId);
        setAtomicAll(_o_._AtomicAll);
        _unknown_ = null;
    }

    public void assign(BTryDistribute _o_) {
        setDistributeId(_o_.getDistributeId());
        setAtomicAll(_o_.isAtomicAll());
        _unknown_ = _o_._unknown_;
    }

    public BTryDistribute copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTryDistribute copy() {
        var _c_ = new BTryDistribute();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTryDistribute _a_, BTryDistribute _b_) {
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
        _s_.append("Zeze.Builtin.HotDistribute.BTryDistribute: {\n");
        _s_.append(_i1_).append("DistributeId=").append(getDistributeId()).append(",\n");
        _s_.append(_i1_).append("AtomicAll=").append(isAtomicAll()).append('\n');
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
            long _x_ = getDistributeId();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isAtomicAll();
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
            setDistributeId(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setAtomicAll(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTryDistribute))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTryDistribute)_o_;
        if (getDistributeId() != _b_.getDistributeId())
            return false;
        if (isAtomicAll() != _b_.isAtomicAll())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getDistributeId() < 0)
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
                case 1: _DistributeId = _v_.longValue(); break;
                case 2: _AtomicAll = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setDistributeId(_r_.getLong(_pn_ + "DistributeId"));
        setAtomicAll(_r_.getBoolean(_pn_ + "AtomicAll"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "DistributeId", getDistributeId());
        _s_.appendBoolean(_pn_ + "AtomicAll", isAtomicAll());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "DistributeId", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "AtomicAll", "bool", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -555413257891539268L;

    private long _DistributeId;
    private boolean _AtomicAll;

    public long getDistributeId() {
        return _DistributeId;
    }

    public void setDistributeId(long _v_) {
        _DistributeId = _v_;
    }

    public boolean isAtomicAll() {
        return _AtomicAll;
    }

    public void setAtomicAll(boolean _v_) {
        _AtomicAll = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _DistributeId_, boolean _AtomicAll_) {
        _DistributeId = _DistributeId_;
        _AtomicAll = _AtomicAll_;
    }

    @Override
    public void reset() {
        _DistributeId = 0;
        _AtomicAll = false;
    }

    @Override
    public Zeze.Builtin.HotDistribute.BTryDistribute toBean() {
        var _b_ = new Zeze.Builtin.HotDistribute.BTryDistribute();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BTryDistribute)_o_);
    }

    public void assign(BTryDistribute _o_) {
        _DistributeId = _o_.getDistributeId();
        _AtomicAll = _o_.isAtomicAll();
    }

    public void assign(BTryDistribute.Data _o_) {
        _DistributeId = _o_._DistributeId;
        _AtomicAll = _o_._AtomicAll;
    }

    @Override
    public BTryDistribute.Data copy() {
        var _c_ = new BTryDistribute.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTryDistribute.Data _a_, BTryDistribute.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BTryDistribute.Data clone() {
        return (BTryDistribute.Data)super.clone();
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
        _s_.append("Zeze.Builtin.HotDistribute.BTryDistribute: {\n");
        _s_.append(_i1_).append("DistributeId=").append(_DistributeId).append(",\n");
        _s_.append(_i1_).append("AtomicAll=").append(_AtomicAll).append('\n');
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
            long _x_ = _DistributeId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = _AtomicAll;
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
            _DistributeId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _AtomicAll = _o_.ReadBool(_t_);
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
        if (!(_o_ instanceof BTryDistribute.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTryDistribute.Data)_o_;
        if (_DistributeId != _b_._DistributeId)
            return false;
        if (_AtomicAll != _b_._AtomicAll)
            return false;
        return true;
    }
}
}
