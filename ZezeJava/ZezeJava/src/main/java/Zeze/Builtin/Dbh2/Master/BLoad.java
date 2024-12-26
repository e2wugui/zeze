// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment"})
public final class BLoad extends Zeze.Transaction.Bean implements BLoadReadOnly {
    public static final long TYPEID = -4353513939405897032L;

    private double _Load;

    private static final java.lang.invoke.VarHandle vh_Load;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Load = _l_.findVarHandle(BLoad.class, "_Load", double.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public double getLoad() {
        if (!isManaged())
            return _Load;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Load;
        var log = (Zeze.Transaction.Logs.LogDouble)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Load;
    }

    public void setLoad(double _v_) {
        if (!isManaged()) {
            _Load = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogDouble(this, 1, vh_Load, _v_));
    }

    @SuppressWarnings("deprecation")
    public BLoad() {
    }

    @SuppressWarnings("deprecation")
    public BLoad(double _Load_) {
        _Load = _Load_;
    }

    @Override
    public void reset() {
        setLoad(0);
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BLoad.Data toData() {
        var _d_ = new Zeze.Builtin.Dbh2.Master.BLoad.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Dbh2.Master.BLoad.Data)_o_);
    }

    public void assign(BLoad.Data _o_) {
        setLoad(_o_._Load);
        _unknown_ = null;
    }

    public void assign(BLoad _o_) {
        setLoad(_o_.getLoad());
        _unknown_ = _o_._unknown_;
    }

    public BLoad copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BLoad copy() {
        var _c_ = new BLoad();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoad _a_, BLoad _b_) {
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BLoad: {\n");
        _s_.append(_i1_).append("Load=").append(getLoad()).append('\n');
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
            double _x_ = getLoad();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
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
            setLoad(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BLoad))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoad)_o_;
        if (getLoad() != _b_.getLoad())
            return false;
        return true;
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
                case 1: _Load = _v_.doubleValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setLoad(_r_.getDouble(_pn_ + "Load"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendDouble(_pn_ + "Load", getLoad());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Load", "double", "", ""));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -4353513939405897032L;

    private double _Load;

    public double getLoad() {
        return _Load;
    }

    public void setLoad(double _v_) {
        _Load = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
    }

    @SuppressWarnings("deprecation")
    public Data(double _Load_) {
        _Load = _Load_;
    }

    @Override
    public void reset() {
        _Load = 0;
    }

    @Override
    public Zeze.Builtin.Dbh2.Master.BLoad toBean() {
        var _b_ = new Zeze.Builtin.Dbh2.Master.BLoad();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BLoad)_o_);
    }

    public void assign(BLoad _o_) {
        _Load = _o_.getLoad();
    }

    public void assign(BLoad.Data _o_) {
        _Load = _o_._Load;
    }

    @Override
    public BLoad.Data copy() {
        var _c_ = new BLoad.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BLoad.Data _a_, BLoad.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BLoad.Data clone() {
        return (BLoad.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Dbh2.Master.BLoad: {\n");
        _s_.append(_i1_).append("Load=").append(_Load).append('\n');
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
            double _x_ = _Load;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Load = _o_.ReadDouble(_t_);
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
        if (!(_o_ instanceof BLoad.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLoad.Data)_o_;
        if (_Load != _b_._Load)
            return false;
        return true;
    }
}
}
