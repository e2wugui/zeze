// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BBatchTid extends Zeze.Transaction.Bean implements BBatchTidReadOnly {
    public static final long TYPEID = -8862994320894252651L;

    private long _Tid;

    private static final java.lang.invoke.VarHandle vh_Tid;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Tid = _l_.findVarHandle(BBatchTid.class, "_Tid", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getTid() {
        if (!isManaged())
            return _Tid;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Tid;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Tid;
    }

    public void setTid(long _v_) {
        if (!isManaged()) {
            _Tid = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Tid, _v_));
    }

    @SuppressWarnings("deprecation")
    public BBatchTid() {
    }

    @SuppressWarnings("deprecation")
    public BBatchTid(long _Tid_) {
        _Tid = _Tid_;
    }

    @Override
    public void reset() {
        setTid(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatchTid.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.BBatchTid.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.BBatchTid.Data)_o_);
    }

    public void assign(BBatchTid.Data _o_) {
        setTid(_o_._Tid);
        _unknown_ = null;
    }

    public void assign(BBatchTid _o_) {
        setTid(_o_.getTid());
        _unknown_ = _o_._unknown_;
    }

    public BBatchTid copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BBatchTid copy() {
        var _c_ = new BBatchTid();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatchTid _a_, BBatchTid _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.BBatchTid: {\n");
        _s_.append(_i1_).append("Tid=").append(getTid()).append('\n');
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
            long _x_ = getTid();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
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
            setTid(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BBatchTid))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatchTid)_o_;
        if (getTid() != _b_.getTid())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getTid() < 0)
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
                case 1: _Tid = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setTid(_r_.getLong(_pn_ + "Tid"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Tid", getTid());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Tid", "long", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -8862994320894252651L;

    private long _Tid;

    public long getTid() {
        return _Tid;
    }

    public void setTid(long _v_) {
        _Tid = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(long _Tid_) {
        _Tid = _Tid_;
    }

    @Override
    public void reset() {
        _Tid = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.BBatchTid toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.BBatchTid();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BBatchTid)_o_);
    }

    public void assign(BBatchTid _o_) {
        _Tid = _o_.getTid();
    }

    public void assign(BBatchTid.Data _o_) {
        _Tid = _o_._Tid;
    }

    @Override
    public BBatchTid.Data copy() {
        var _c_ = new BBatchTid.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BBatchTid.Data _a_, BBatchTid.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BBatchTid.Data clone() {
        return (BBatchTid.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.BBatchTid: {\n");
        _s_.append(_i1_).append("Tid=").append(_Tid).append('\n');
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
            long _x_ = _Tid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Tid = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BBatchTid.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BBatchTid.Data)_o_;
        if (_Tid != _b_._Tid)
            return false;
        return true;
    }
}
}
